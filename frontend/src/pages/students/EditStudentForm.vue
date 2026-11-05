<template>
  <el-form
    ref="studentForm"
    :model="studentBeingEdited"
    :rules="rules"
    label-width="110px"
    style="padding-right: 30px"
    label-position="left"
    :disabled="busy || expectedEnabled === undefined"
  >
    <el-form-item label="Id:">
      <el-input v-model="studentBeingEdited.id" disabled></el-input>
    </el-form-item>
    <el-form-item label="Username:" prop="username">
      <el-input v-model="studentBeingEdited.username"></el-input>
    </el-form-item>
    <el-form-item label="Status:" prop="enabled">
      <el-tag v-if="expectedEnabled === true" type="success">Active</el-tag>
      <el-switch
        v-else-if="expectedEnabled === false"
        v-model="studentBeingEdited.enabled"
        inline-prompt
        active-text="Enabled"
        inactive-text="Disabled"
        style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
      />
    </el-form-item>
    <el-form-item label="First Name:" prop="firstName">
      <el-input v-model="studentBeingEdited.firstName"></el-input>
    </el-form-item>
    <el-form-item label="Last Name:" prop="lastName">
      <el-input v-model="studentBeingEdited.lastName"></el-input>
    </el-form-item>
    <el-form-item label="Email:" prop="email">
      <el-input v-model="studentBeingEdited.email"></el-input>
    </el-form-item>
    <el-form-item label="Team:" prop="teamName">
      <el-input v-model="studentBeingEdited.teamName" disabled></el-input>
    </el-form-item>
    <div style="display: flex; justify-content: end">
      <el-button
        v-if="expectedEnabled === true"
        type="danger"
        :loading="busy"
        @click="confirmDeactivation"
        >Deactivate student</el-button
      >
      <el-button @click="$emit('close-dialog')"> Cancel </el-button>
      <el-button type="primary" @click="updateExistingStudent()"> Confirm </el-button>
    </div>
  </el-form>
  <el-button v-if="expectedEnabled === undefined && !busy" @click="reloadStudent">
    Reload student
  </el-button>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { isAxiosError } from 'axios'
import type { Student } from '@/apis/student/types'
import type { FormInstance } from 'element-plus'
import { deactivateStudent, findStudentById, updateStudent } from '@/apis/student'
import { ref, onMounted } from 'vue'

const { studentId } = defineProps<{ studentId: number }>()
const emit = defineEmits(['close-dialog'])
const expectedEnabled = ref<boolean>()
const busy = ref(false)

// Note that the structure of the userData is NOT the same as the User interface since the roles are stored as an array
const studentBeingEdited = ref<Student>({
  // Data of the user being added or edited
  id: NaN,
  username: '',
  enabled: false,
  firstName: '',
  lastName: '',
  email: ''
})

const studentForm = ref<FormInstance>() // Form reference

// Validation rules
const rules = {
  username: [
    { required: true, message: 'Please input your email as your username', trigger: 'blur' },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: 'Enter your email as username.',
      trigger: 'blur'
    }
  ],
  firstName: [
    { required: true, message: 'Please provide the first name of the user.', trigger: 'blur' }
  ],
  lastName: [
    { required: true, message: 'Please provide the last name of the user.', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please provide the email of the user.', trigger: 'blur' },
    { type: 'email', message: 'Please provide a valid email.', trigger: 'blur' }
  ]
}

async function loadStudent() {
  expectedEnabled.value = undefined
  const { data } = await findStudentById(studentId)
  studentBeingEdited.value = { ...data }
  expectedEnabled.value = data.enabled
}

async function reloadStudent() {
  busy.value = true
  try {
    await loadStudent()
  } catch {
    // The request interceptor reports the error. Keep writes disabled until a reload succeeds.
  } finally {
    busy.value = false
  }
}

onMounted(reloadStudent)

async function updateExistingStudent() {
  if (busy.value || expectedEnabled.value === undefined) return
  const originalStatus = expectedEnabled.value
  busy.value = true
  try {
    await studentForm.value!.validate()

    await updateStudent(studentBeingEdited.value, originalStatus)
    ElMessage.success('Student updated successfully')
    emit('close-dialog')
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 409) {
      await reloadStudent()
    }
  } finally {
    busy.value = false
  }
}

async function confirmDeactivation() {
  if (busy.value || expectedEnabled.value !== true) return
  busy.value = true
  try {
    try {
      await ElMessageBox.confirm(
        `Deactivate ${studentBeingEdited.value.firstName} ${studentBeingEdited.value.lastName}? ` +
          'They will lose access, including existing sessions, and stop receiving submission reminders. ' +
          'Teammates will no longer be asked to evaluate them. Their team membership and submitted work will be kept.',
        'Deactivate student',
        { type: 'warning', confirmButtonText: 'Deactivate', cancelButtonText: 'Cancel' }
      )
    } catch {
      return
    }
    try {
      const result = await deactivateStudent(studentId)
      ElMessage.success(result.message)
      emit('close-dialog')
    } catch (error) {
      const status = isAxiosError(error) ? error.response?.status : undefined
      if (status === undefined || status === 408 || status >= 500) {
        ElMessage.warning(
          'Deactivation could not be confirmed. Checking the current account status.'
        )
        await reloadStudent()
      }
    }
  } finally {
    busy.value = false
  }
}
</script>

<style scoped lang="scss"></style>
