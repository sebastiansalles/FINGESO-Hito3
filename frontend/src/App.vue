<script setup>
import { ref, onMounted } from 'vue'

const USUARIO_ID = 2   // el estudiante que crea el DataSeeder

const hitos = ref([])
const archivos = ref({})
const mensaje = ref('')
const error = ref('')
const enviando = ref(false)

async function cargarPanel() {
  const respuesta = await fetch('/api/panel', {
    headers: { 'X-Usuario-Id': USUARIO_ID }
  })
  hitos.value = await respuesta.json()
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
  const respuesta = await fetch('/api/entregas', {
    method: 'POST',
    headers: { 'X-Usuario-Id': USUARIO_ID },
    body: datos
  })
  enviando.value = false

  const cuerpo = await respuesta.json()

  if (respuesta.ok) {
    mensaje.value = 'Entrega registrada: ' + cuerpo.nombreArchivo
    await cargarPanel()
  } else {
    error.value = cuerpo.mensaje
  }
}

function formatear(fecha) {
  if (!fecha) return '—'
  return new Date(fecha).toLocaleString('es-CL')
}

onMounted(cargarPanel)
</script>

<template>
  <main>
    <h1>Mis Entregas</h1>
    <p class="sub">Plataforma de Gestión de Tesistas</p>

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
        <td>{{ hito.nombreHito }}</td>
        <td>
          {{ formatear(hito.fechaLimite) }}
          <span v-if="!hito.plazoVigente" class="vencido">vencido</span>
        </td>
        <td>{{ hito.estadoEntrega || 'Sin entrega' }}</td>
        <td>
            <span v-if="hito.nombreArchivo">
              {{ hito.nombreArchivo }}<br>
              <small>{{ formatear(hito.fechaHoraCarga) }}</small>
            </span>
          <span v-else>—</span>
        </td>
        <td>
          <input type="file" @change="seleccionarArchivo(hito.hitoId, $event)">
          <button :disabled="enviando" @click="enviarEntrega(hito.hitoId)">
            {{ enviando ? 'Enviando...' : 'Enviar' }}
          </button>
        </td>
      </tr>
      </tbody>
    </table>
  </main>
</template>

<style>
body { font-family: system-ui, sans-serif; background: #f7f7f8; margin: 0; }
main { max-width: 950px; margin: 40px auto; padding: 0 20px; }
h1 { margin-bottom: 4px; }
.sub { color: #666; margin-top: 0; }
table { width: 100%; border-collapse: collapse; background: #fff; margin-top: 20px; }
th, td { padding: 12px; border-bottom: 1px solid #e5e5e5; text-align: left; font-size: 14px; }
th { background: #0d6b5f; color: #fff; }
button { padding: 6px 14px; margin-left: 8px; cursor: pointer; }
.ok  { background: #e6f5ea; color: #1c6b32; padding: 12px; border-radius: 6px; }
.err { background: #fdeaea; color: #a32020; padding: 12px; border-radius: 6px; }
.vencido { color: #a32020; font-weight: 600; margin-left: 6px; }
small { color: #888; }
</style>