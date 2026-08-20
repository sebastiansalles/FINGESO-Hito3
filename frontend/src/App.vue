<script setup>
import { ref, onMounted } from 'vue'

const TAMANO_MAXIMO_MB = 20

const usuario = ref(null)
const rut = ref('12345678-3')
const contrasena = ref('')
const errorLogin = ref('')

const tesis = ref(null)
const hitos = ref([])
const abierto = ref(null)         // id del hito desplegado, o null
const archivo = ref(null)
const comentario = ref('')
const claveArchivo = ref(0)       // fuerza a limpiar el input tras enviar
const mensaje = ref('')
const error = ref('')
const enviando = ref(false)

function esEstudiante() {
  return usuario.value && usuario.value.rol === 'ESTUDIANTE'
}

async function cargarDatosDelEstudiante() {
  await cargarTesis()
  await cargarPanel()
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
    if (esEstudiante()) await cargarDatosDelEstudiante()
  } else {
    const cuerpo = await respuesta.json()
    errorLogin.value = cuerpo.mensaje || 'No se pudo iniciar sesión.'
  }
}

async function cerrarSesion() {
  await fetch('/api/sesion', { method: 'DELETE' })
  usuario.value = null
  tesis.value = null
  hitos.value = []
  contrasena.value = ''
  abierto.value = null
  mensaje.value = ''
  error.value = ''
}

async function cargarTesis() {
  const respuesta = await fetch('/api/tesis')
  tesis.value = respuesta.ok ? await respuesta.json() : null
}

async function cargarPanel() {
  error.value = ''
  const respuesta = await fetch('/api/panel')
  if (respuesta.ok) {
    hitos.value = await respuesta.json()
  } else {
    const cuerpo = await respuesta.json()
    error.value = cuerpo.mensaje || 'No se pudieron cargar los hitos.'
  }
}

function alternar(hito) {
  abierto.value = (abierto.value === hito.hitoId) ? null : hito.hitoId
  archivo.value = null
  comentario.value = ''
  mensaje.value = ''
  error.value = ''
}

function elegirArchivo(evento) {
  error.value = ''
  const seleccionado = evento.target.files[0]

  if (!seleccionado) {
    archivo.value = null
    return
  }

  const pesoMb = seleccionado.size / (1024 * 1024)
  if (pesoMb > TAMANO_MAXIMO_MB) {
    error.value = `El archivo pesa ${pesoMb.toFixed(1)} MB. El máximo es ${TAMANO_MAXIMO_MB} MB.`
    archivo.value = null
    claveArchivo.value++
    return
  }

  archivo.value = seleccionado
}

async function enviarEntrega(hito) {
  mensaje.value = ''
  error.value = ''

  if (!archivo.value) {
    error.value = 'Debe adjuntar un archivo.'
    return
  }

  const pesoMb = archivo.value.size / (1024 * 1024)
  if (pesoMb > TAMANO_MAXIMO_MB) {
    error.value = `El archivo pesa ${pesoMb.toFixed(1)} MB. El máximo es ${TAMANO_MAXIMO_MB} MB.`
    return
  }

  const datos = new FormData()
  datos.append('hitoId', hito.hitoId)
  datos.append('archivo', archivo.value)
  datos.append('comentario', comentario.value)

  enviando.value = true
  let respuesta
  try {
    respuesta = await fetch('/api/entregas', { method: 'POST', body: datos })
  } catch (e) {
    enviando.value = false
    error.value = 'No se pudo completar el envío. Intente nuevamente.'
    return
  }
  enviando.value = false

  const cuerpo = await respuesta.json()

  if (respuesta.ok) {
    mensaje.value = 'Entrega registrada: ' + cuerpo.nombreArchivo
    archivo.value = null
    comentario.value = ''
    claveArchivo.value++
    await cargarPanel()
  } else {
    error.value = cuerpo.mensaje || 'No se pudo registrar la entrega.'
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

function etiqueta(hito) {
  if (hito.estadoEntrega === 'EVALUADO') return 'Evaluado'
  if (hito.estadoEntrega === 'ENVIADO_PARA_REVISION') return 'En revisión'
  return hito.plazoVigente ? 'Pendiente' : 'No entregado'
}

function colorEstado(hito) {
  if (hito.estadoEntrega === 'EVALUADO') return '#1a4fa0'
  if (hito.estadoEntrega === 'ENVIADO_PARA_REVISION') return '#1c6b32'
  return hito.plazoVigente ? '#666' : '#a32020'
}

onMounted(async () => {
  const respuesta = await fetch('/api/sesion')
  if (respuesta.ok) {
    usuario.value = await respuesta.json()
    if (esEstudiante()) await cargarDatosDelEstudiante()
  }
})
</script>

<template>
  <div style="max-width: 800px; margin: 0 auto;">

    <!-- Inicio de sesión -->
    <div v-if="!usuario">
      <h2>Plataforma de Gestión de Tesistas</h2>
      <div>
        <input v-model="rut" placeholder="RUT">
        <input v-model="contrasena" type="password" placeholder="Contraseña" @keyup.enter="iniciarSesion">
        <button @click="iniciarSesion">Ingresar</button>
      </div>
      <p v-if="errorLogin" style="color: #a32020;">{{ errorLogin }}</p>
    </div>

    <!-- Otro perfil -->
    <div v-else-if="!esEstudiante()">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h2>{{ usuario.nombre }}</h2>
        <button @click="cerrarSesion">Cerrar sesión</button>
      </div>
      <p>Perfil activo: <strong>{{ usuario.rol }}</strong>.</p>
      <p>Solo el perfil Estudiante tiene funcionalidades habilitadas en esta demo.</p>
    </div>

    <!-- Panel del estudiante -->
    <div v-else>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <h2>Mis Entregas — {{ usuario.nombre }}</h2>
        <button @click="cerrarSesion">Cerrar sesión</button>
      </div>

      <div v-if="tesis" style="border-bottom: 2px solid #0d6b5f; margin-bottom: 20px; padding-bottom: 10px;">
        <strong>Tesis:</strong> {{ tesis.titulo }}<br>
        <strong>Guía:</strong> {{ tesis.profesorGuia }} | <strong>Estado:</strong> {{ tesis.estado }}
      </div>

      <p v-if="mensaje" style="color: #1c6b32; font-weight: bold;">{{ mensaje }}</p>
      <p v-if="error" style="color: #a32020; font-weight: bold;">{{ error }}</p>

      <!-- Acordeón de Hitos -->
      <div>
        <div v-for="hito in hitos" :key="hito.hitoId" style="border: 1px solid #d5d8dc; margin-bottom: 10px;">

          <!-- Cabecera -->
          <div @click="alternar(hito)" style="cursor: pointer; background: #f0f2f4; padding: 10px; display: flex; justify-content: space-between; align-items: center; gap: 10px;">
            <strong style="display: flex; align-items: center; gap: 8px; min-width: 0;">
              <span :style="{
                      flex: 'none',
                      color: '#666',
                      fontSize: '11px',
                      display: 'inline-block',
                      transition: 'transform .15s',
                      transform: abierto === hito.hitoId ? 'rotate(90deg)' : 'none'
                    }">▶</span>
              <span>{{ hito.nombreHito }}</span>
            </strong>
            <span :style="{ color: colorEstado(hito), whiteSpace: 'nowrap', flex: 'none', fontWeight: '600' }">
              {{ etiqueta(hito) }}
            </span>
          </div>

          <!-- Detalle -->
          <div v-if="abierto === hito.hitoId" style="padding: 15px;">

            <p>
              <strong>Plazo:</strong> {{ formatear(hito.fechaLimite) }}
              <strong v-if="!hito.plazoVigente" style="color: #a32020;">[Cerrado]</strong>
            </p>

            <div style="margin-bottom: 15px;">
              <strong>Documento actual:</strong>
              <div v-if="hito.nombreArchivo">
                {{ hito.nombreArchivo }} (Subido el {{ formatear(hito.fechaHoraCarga) }})
                <div v-if="hito.comentario" style="font-style: italic;">Comentario: "{{ hito.comentario }}"</div>
              </div>
              <div v-else>Sin entrega registrada.</div>
            </div>

            <!-- Formulario -->
            <div v-if="puedeEnviar(hito)" style="border: 1px solid #d5d8dc; padding: 15px; background: #f9f9f9;">
              <label>Subir archivo (PDF o DOCX, máx 20 MB):</label><br>
              <input type="file" accept=".pdf,.docx" :key="claveArchivo" @change="elegirArchivo" style="display: block; max-width: 100%; margin-top: 5px;"><br>

              <label>Comentario (opcional):</label><br>
              <textarea v-model="comentario" rows="3" style="width: 100%; box-sizing: border-box;"></textarea><br>

              <button :disabled="enviando" @click="enviarEntrega(hito)" style="margin-top: 10px; padding: 5px 15px;">
                {{ enviando ? 'Enviando...' : (hito.nombreArchivo ? 'Reemplazar entrega' : 'Enviar entrega') }}
              </button>
            </div>

            <div v-else style="background: #f7f7f7; padding: 10px; border: 1px solid #e0e0e0;">
              <span v-if="!hito.plazoVigente">El plazo de este hito ya venció y no permite subidas.</span>
              <span v-else>La entrega ya fue evaluada.</span>
            </div>

          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style>
html {
  overflow-y: scroll;
}
body {
  font-family: sans-serif;
  margin: 0;
  padding: 20px;
  overflow-wrap: anywhere;
}
input, button {
  margin-right: 5px;
  padding: 5px;
  max-width: 100%;
}
</style>