<script setup>
import { ref, onMounted } from 'vue'

const usuario = ref(null)
const rut = ref('22222222-2')
const contrasena = ref('')
const errorLogin = ref('')

const hitos = ref([])
const archivos = ref({})
const mensaje = ref('')
const error = ref('')
const enviando = ref(false)

async function iniciarSesion() {
  errorLogin.value = ''
  const respuesta = await fetch('/api/sesion', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ rut: rut.value, contrasena: contrasena.value })
  })
  const cuerpo = await respuesta.json()

  if (respuesta.ok) {
    usuario.value = cuerpo
    await cargarPanel()
  } else {
    errorLogin.value = cuerpo.mensaje
  }
}

async function cerrarSesion() {
  await fetch('/api/sesion', { method: 'DELETE' })
  usuario.value = null
  hitos.value = []
  contrasena.value = ''
}

async function cargarPanel() {
  const respuesta = await fetch('/api/panel')
  if (respuesta.ok) {
    hitos.value = await respuesta.json()
  } else {
    error.value = 'No fue posible cargar los hitos.'
  }
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
  const respuesta = await fetch('/api/entregas', { method: 'POST', body: datos })
  enviando.value = false

  const cuerpo = await respuesta.json()

  if (respuesta.ok) {
    mensaje.value = 'Entrega registrada: ' + cuerpo.nombreArchivo
    archivos.value[hitoId] = null
    await cargarPanel()
  } else {
    error.value = cuerpo.mensaje
  }
}

function formatear(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleString('es-CL', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit'
  })
}

function puedeEnviar(hito) {
  return hito.plazoVigente && hito.estadoEntrega !== 'EVALUADO'
}

// Si ya había una sesión abierta, la recupera al recargar la página
onMounted(async () => {
  const respuesta = await fetch('/api/sesion')
  if (respuesta.ok) {
    usuario.value = await respuesta.json()
    await cargarPanel()
  }
})
</script>

<template>
  <!-- Pantalla de login -->
  <main v-if="!usuario" class="login">
    <h1>Plataforma de Gestión de Tesistas</h1>
    <p class="sub">Ingrese con sus credenciales institucionales</p>

    <label>RUT</label>
    <input v-model="rut" type="text" placeholder="12345678-9">

    <label>Contraseña</label>
    <input v-model="contrasena" type="password" @keyup.enter="iniciarSesion">

    <button @click="iniciarSesion">Ingresar</button>

    <p v-if="errorLogin" class="err">{{ errorLogin }}</p>
  </main>

  <!-- Panel de entregas -->
  <main v-else>
    <header class="barra">
      <div>
        <h1>Mis Entregas</h1>
        <p class="sub">{{ usuario.nombre }} · {{ usuario.rol }}</p>
      </div>
      <button class="secundario" @click="cerrarSesion">Cerrar sesión</button>
    </header>

    <p v-if="mensaje" class="ok">{{ mensaje }}</p>
    <p v-if="error" class="err">{{ error }}</p>

    <table>
      <thead>
      <tr>
        <th>Hito</th>
        <th>Plazo</th>
        <th>Estado</th>
        <th>Documento</th>
        <th>Enviar</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="hito in hitos" :key="hito.hitoId">
        <td class="nombre">{{ hito.nombreHito }}</td>
        <td>
          {{ formatear(hito.fechaLimite) }}
          <span v-if="!hito.plazoVigente" class="etiqueta vencido">Vencido</span>
        </td>
        <td>
          <span v-if="hito.estadoEntrega === 'EVALUADO'" class="etiqueta evaluado">Evaluado</span>
          <span v-else-if="hito.estadoEntrega" class="etiqueta enviado">En revisión</span>
          <span v-else class="apagado">Sin entrega</span>
        </td>
        <td>
            <span v-if="hito.nombreArchivo">
              {{ hito.nombreArchivo }}<br>
              <small>{{ formatear(hito.fechaHoraCarga) }}</small>
            </span>
          <span v-else class="apagado">—</span>
        </td>
        <td>
          <template v-if="puedeEnviar(hito)">
            <input type="file" @change="seleccionarArchivo(hito.hitoId, $event)">
            <button :disabled="enviando" @click="enviarEntrega(hito.hitoId)">
              {{ enviando ? 'Enviando...' : 'Enviar' }}
            </button>
          </template>
          <span v-else class="apagado">No disponible</span>
        </td>
      </tr>
      </tbody>
    </table>

    <p class="pie">PDF o DOCX · máximo 20 MB</p>
  </main>
</template>

<style>
:root {
  --verde: #0d6b5f;
  --texto: #1f2328;
  --gris: #6b7280;
  --borde: #e5e7eb;
}

body {
  margin: 0;
  background: #f6f7f9;
  color: var(--texto);
  font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
}

main {
  max-width: 940px;
  margin: 40px auto;
  padding: 0 24px;
}

h1 { font-size: 24px; margin: 0 0 2px; }
.sub { color: var(--gris); margin: 0; font-size: 14px; }

.barra {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

/* --- Login --- */
.login {
  max-width: 360px;
  margin-top: 12vh;
  background: #fff;
  padding: 32px;
  border: 1px solid var(--borde);
  border-radius: 8px;
}
.login h1 { font-size: 20px; }
.login label {
  display: block;
  margin: 18px 0 6px;
  font-size: 13px;
  font-weight: 600;
}
.login input {
  width: 100%;
  padding: 9px 11px;
  border: 1px solid var(--borde);
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}
.login input:focus {
  outline: none;
  border-color: var(--verde);
}
.login button { width: 100%; margin-top: 22px; padding: 10px; }

/* --- Tabla --- */
table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border: 1px solid var(--borde);
  border-radius: 8px;
  overflow: hidden;
}
th {
  background: var(--verde);
  color: #fff;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: .4px;
}
th, td {
  padding: 13px 16px;
  text-align: left;
  font-size: 14px;
  vertical-align: top;
}
tbody tr:not(:last-child) td { border-bottom: 1px solid var(--borde); }
.nombre { font-weight: 600; }
.apagado { color: var(--gris); }
small { color: var(--gris); }

/* --- Etiquetas de estado --- */
.etiqueta {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 11px;
  font-size: 12px;
  font-weight: 600;
}
.vencido  { background: #fdeaea; color: #a32020; margin-left: 6px; }
.evaluado { background: #e8f0fe; color: #1a4fa0; }
.enviado  { background: #e6f5ea; color: #1c6b32; }

/* --- Botones --- */
button {
  background: var(--verde);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 7px 15px;
  font-size: 14px;
  cursor: pointer;
}
button:hover { background: #0a564c; }
button:disabled { background: #c8ccd0; cursor: not-allowed; }
.secundario {
  background: #fff;
  color: var(--texto);
  border: 1px solid var(--borde);
}
.secundario:hover { background: #f3f4f6; }

input[type="file"] { font-size: 13px; max-width: 190px; }

/* --- Mensajes --- */
.ok, .err {
  padding: 11px 14px;
  border-radius: 6px;
  font-size: 14px;
  margin-bottom: 18px;
}
.ok  { background: #e6f5ea; color: #1c6b32; border: 1px solid #bce0c8; }
.err { background: #fdeaea; color: #a32020; border: 1px solid #f2c4c4; }

.pie { color: var(--gris); font-size: 13px; margin-top: 14px; }
</style>