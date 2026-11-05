/**
 * 全站通用的内联 SVG 占位图（data URI）
 * 说明：项目已统一改为内联 SVG，不再依赖任何外网占位图服务（如 via.placeholder.com），
 * 避免图片地址失效或受网络策略影响。生成的 data URI 可直接用于 <img>、el-image 的 src。
 */

/**
 * 生成一张内联 SVG 占位图
 * @param {string} text 占位文案
 * @param {string} bg 背景色
 * @param {string} fg 文字颜色
 * @returns {string} data URI
 */
export const buildPlaceholder = (
  text = '暂无封面',
  bg = '#f0f2f5',
  fg = '#c0c4cc'
) => {
  const svg =
    '<svg xmlns="http://www.w3.org/2000/svg" width="300" height="180" viewBox="0 0 300 180">' +
    `<rect width="300" height="180" fill="${bg}"/>` +
    `<text x="150" y="95" font-family="sans-serif" font-size="16" fill="${fg}" text-anchor="middle">${text}</text>` +
    '</svg>'
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
}

/** 默认占位图（活动封面、订单封面等通用场景） */
export const PLACEHOLDER_IMAGE = buildPlaceholder('暂无封面')

/** 图片加载失败的兜底文案版本 */
export const PLACEHOLDER_IMAGE_ERROR = buildPlaceholder('图片加载失败', '#eef1f6', '#b6bcc7')

export default PLACEHOLDER_IMAGE
