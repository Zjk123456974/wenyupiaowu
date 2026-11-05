import request from '@/utils/request'

// 借鉴大麦「电子票转赠」能力：发起转赠 / 接收转赠 / 取消转赠 / 查询我发起的转赠
export function initiateTransfer(data) {
  return request.post('/ticket/transfer/initiate', data)
}

export function acceptTransfer(data) {
  return request.post('/ticket/transfer/accept', data)
}

export function cancelTransfer(data) {
  return request.post('/ticket/transfer/cancel', data)
}

export function getMySentTransfers() {
  return request.get('/ticket/transfer/my-sent')
}
