import request from '@/utils/request'

/**
 * 抢票（支持一单多观演人）
 * @param {Object} data { activityId, quantity, ticketPriceId?, attendees?: [{ attendeeName, attendeeIdCard }] }
 */
export function grabTicket(data) {
  return request.post('/ticket/grab', data)
}
