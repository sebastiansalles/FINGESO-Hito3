<script setup>
import { ref, onMounted } from 'vue'

const usuario = ref(null)
const rut = ref('22222222-2')
const contrasena = ref('')
const errorLogin = ref('')

const hitos = ref([])
const hitoSeleccionado = ref(null)
const archivos = ref({})
const mensaje = ref('')
const error = ref('')
const enviando = ref(false)

function esEstudiante() {
  return usuario.value && usuario.value.rol === 'ESTUDIANTE'
}

async function iniciarSesion() {
  errorLogin.value = ''
  const respuesta = await fetch('/api/sesion', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ rut: rut.value, contrasena: contrasena.value })
  })

  if (respuesta.ok) {
    usuario.value = await respuesta.json()
    if (esEstudiante()) await cargarPanel()
  } else {
    const cuerpo = await respuesta.json()
    errorLogin.value = cuerpo.mensaje || 'Error al iniciar sesión'
  }
}

async function cerrarSesion() {
  await fetch('/api/sesion', { method: 'DELETE' })
  usuario.value = null
  hitos.value = []
  contrasena.value = ''
  hitoSeleccionado.value = null
  mensaje.value = ''
  error.value = ''
}

async function cargarPanel() {
  error.value = ''
  const respuesta = await fetch('/api/panel')
  if (respuesta.ok) {
    hitos.value = await respuesta.json()
  } else {
    const cuerpo = await respuesta.json()
    error.value = cuerpo.mensaje || 'No fue posible cargar los hitos.'
  }
}

// Paso 1 del CU: el estudiante selecciona un hito
function seleccionarHito(hitoId) {
  hitoSeleccionado.value = hitoId
  mensaje.value = ''
  error.value = ''
}

function cancelar() {
  hitoSeleccionado.value = null
}

function seleccionarArchivo(hitoId, evento) {
  archivos.value[hitoId] = evento.target.files[0]
}

async function enviarEntrega(hitoId) {
  mensaje.value = ''
  error.value = ''

  const archivo = archivos.value[hitoId]
  if (!archivo) {
    error.value = 'Debe seleccionar un archivo.'
    return
  }

  const datos = new FormData()
  datos.append('hitoId', hitoId)
  datos.append('archivo', archivo)

  enviando.value = true
  let respuesta
  try {
    respuesta = await fetch('/api/entregas', { method: 'POST', body: datos })
  } catch (e) {
    enviando.value = false
    error.value = 'No fue posible contactar al servidor. Verifique el tamaño del archivo.'
    return
  }
  enviando.value = false

  const cuerpo = await respuesta.json()

  if (respuesta.ok) {
    mensaje.value = 'Entrega registrada: ' + cuerpo.nombreArchivo
    archivos.value[hitoId] = null
    hitoSeleccionado.value = null
    await cargarPanel()
  } else {
    error.value = cuerpo.mensaje || 'Error al enviar'
  }
}

function formatear(fecha) {
  if (!fecha) return ''
  return new Date(fecha).toLocaleString('es-CL', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

function puedeEnviar(hito) {
  return hito.plazoVigente && hito.estadoEntrega !== 'EVALUADO'
}

onMounted(async () => {
  const respuesta = await fetch('/api/sesion')
  if (respuesta.ok) {
    usuario.value = await respuesta.json()
    if (esEstudiante()) await cargarPanel()
  }
})
</script>

<template>
  <div class="contenedor">
    <!-- Login -->
    <div v-if="!usuario">
      <h2>Plataforma de Gestión de Tesistas</h2>
      <div>
        <input v-model="rut" placeholder="RUT">
        <input v-model="contrasena" type="password" placeholder="Contraseña" @keyup.enter="iniciarSesion">
        <button @click="iniciarSesion">Ingresar</button>
      </div>
      <p v-if="errorLogin" style="color: red;">{{ errorLogin }}</p>
    </div>

    <!-- Perfil sin funcionalidad en esta iteración -->
    <div v-else-if="!esEstudiante()">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h2>{{ usuario.nombre }}</h2>
        <button @click="cerrarSesion">Cerrar sesión</button>
      </div>
      <p>
        Sesión iniciada con perfil <strong>{{ usuario.rol }}</strong>.
      </p>
      <p>
        Esta iteración implementa el caso de uso CU_009 (Registrar Entrega Parcial),
        correspondiente al perfil Estudiante. Las funcionalidades de este perfil están
        diseñadas y no implementadas.
      </p>
    </div>

    <!-- Panel del estudiante -->
    <div v-else>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h2>Entregas de {{ usuario.nombre }}</h2>
        <button @click="cerrarSesion">Cerrar sesión</button>
      </div>

      <p v-if="mensaje" style="color: green;">{{ mensaje }}</p>
      <p v-if="error" style="color: red;">{{ error }}</p>

      <table border="1" cellpadding="8" cellspacing="0" style="width: 100%; text-align: left;">
        <thead style="background: #eee;">
        <tr>
          <th>Hito</th>
          <th>Plazo</th>
          <th>Estado</th>
          <th>Documento</th>
          <th>Acción</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="hito in hitos" :key="hito.hitoId">
          <td>{{ hito.nombreHito }}</td>
          <td>
            {{ formatear(hito.fechaLimite) }}
            <strong v-if="!hito.plazoVigente" style="color: red;"> (Vencido)</strong>
          </td>
          <td>{{ hito.estadoEntrega || 'Pendiente' }}</td>
          <td>{{ hito.nombreArchivo || '—' }}</td>
          <td>
            <template v-if="puedeEnviar(hito)">
              <!-- Paso 1: seleccionar el hito -->
              <button v-if="hitoSeleccionado !== hito.hitoId"
                      @click="seleccionarHito(hito.hitoId)">
                Seleccionar
              </button>

              <!-- Paso 4: el sistema despliega el formulario -->
              <div v-else>
                <input type="file" @change="seleccionarArchivo(hito.hitoId, $event)">
                <button :disabled="enviando" @click="enviarEntrega(hito.hitoId)">
                  {{ enviando ? 'Enviando...' : 'Enviar Entrega' }}
                </button>
                <button @click="cancelar">Cancelar</button>
              </div>
            </template>
            <span v-else>No disponible</span>
          </td>
        </tr>
        </tbody>
      </table>

      <p style="color: #666; font-size: 13px;">PDF o DOCX · máximo 20 MB</p>
    </div>
  </div>
</template>

<style>
body {
  font-family: sans-serif;
  margin: 0;
  padding: 20px;
}
.contenedor {
  max-width: 900px;
  margin: 0 auto;
}
input, button {
  margin-right: 5px;
  padding: 5px;
}
</style>