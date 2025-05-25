<template>
  <div class="nli-console">
    <div class="message-list" ref="messageListRef">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message', msg.role]"
      >
        <div class="bubble">
          <strong>{{ msg.role === 'user' ? '你' : 'AI' }}</strong>：{{ msg.content }}
        </div>
      </div>
    </div>

    <div class="input-area">
      <input
        v-model="inputText"
        @keyup.enter="send"
        placeholder="请输入自然语言指令"
      />
      <button @click.prevent="send">发送</button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import { sendNLIRequest } from '@/api/nli'

const inputText = ref('')
const messages = ref(JSON.parse(localStorage.getItem('nli_messages') || '[]'))

watch(messages, () => {
  localStorage.setItem('nli_messages', JSON.stringify(messages.value))
}, { deep: true })

const send = async () => {
  const text = inputText.value.trim()
  if (!text) return
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''

  await nextTick()

  try {
    const response = await sendNLIRequest(text, false)

    if (response.needConfirm) {
      await ElMessageBox.confirm(response.reply || '是否继续执行？', '敏感操作确认')
      const confirmRes = await sendNLIRequest(text, true)
      messages.value.push({ role: 'assistant', content: confirmRes.reply || '✅ 操作成功' })
    } else {
      messages.value.push({ role: 'assistant', content: response.reply || '✅ 操作成功' })
    }
  } catch (err) {
    messages.value.push({ role: 'assistant', content: '❌ 操作失败：' + (err.message || '未知错误') })
  }
}
</script>

<style scoped>
.nli-console {
  display: flex;
  flex-direction: column;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  height: 500px;
  max-width: 700px;
  margin: 0 auto;
  background-color: #fafafa;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fff;
  margin-bottom: 12px;
}

.message {
  margin-bottom: 10px;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 70%;
  padding: 8px 12px;
  border-radius: 8px;
  background-color: #409eff;
  color: #fff;
  word-break: break-word;
}

.message.assistant .bubble {
  background-color: #f0f0f0;
  color: #303133;
}

.input-area {
  display: flex;
  gap: 10px;
}

input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border 0.3s;
}

input:focus {
  border-color: #409eff;
}

button {
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 10px 20px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

button:hover {
  background-color: #66b1ff;
}
</style>
