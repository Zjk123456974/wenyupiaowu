<template>
  <div class="activity-detail-page" v-loading="loading && !!activity">
    <!-- 待支付订单提醒横幅 -->
    <div v-if="pendingOrder" class="pending-order-banner animate-fade-in-up">
      <div class="banner-left">
        <el-icon class="banner-icon"><Warning /></el-icon>
        <div class="banner-content">
          <div class="banner-title">您有一笔待支付订单</div>
          <div class="banner-desc">
            活动：{{ pendingOrder.activityTitle || activity?.title }}
            <span class="banner-sep">·</span>
            数量：{{ pendingOrder.quantity }}张
            <span class="banner-sep">·</span>
            金额：<span class="banner-amount">¥{{ Number(pendingOrder.totalAmount || 0).toFixed(2) }}</span>
          </div>
          <div class="banner-countdown">
            <el-icon><Clock /></el-icon>
            <span>剩余支付时间：</span>
            <span class="countdown-digits">{{ pendingOrderCountdown }}</span>
            <span class="countdown-tip">超时后订单将自动取消</span>
          </div>
        </div>
      </div>
      <div class="banner-right">
        <el-button type="danger" size="large" class="pay-now-btn" @click="goPayPendingOrder">
          立即支付
        </el-button>
        <el-button size="large" plain @click="cancelPendingOrder">取消订单</el-button>
      </div>
    </div>

    <!-- 首屏骨架屏：结构与真实详情页一致，避免加载完成后的布局跳动 -->
    <div v-if="loading && !activity" class="detail-skeleton">
      <div class="skeleton-top">
        <el-skeleton :rows="1" animated class="skeleton-cover">
          <template #template>
            <el-skeleton-item variant="image" style="width: 100%; height: 240px" />
          </template>
        </el-skeleton>
        <div class="skeleton-info">
          <el-skeleton :rows="6" animated />
        </div>
      </div>
      <el-skeleton :rows="4" animated class="skeleton-tabs" />
    </div>

    <div class="detail-main" v-if="activity">
      <!-- 上方信息区 -->
      <div class="detail-top">
        <!-- 左侧封面 -->
        <div class="detail-cover">
          <el-image
            class="cover-image"
            :src="activity.coverImage"
            fit="cover"
            lazy
          >
            <template #error>
              <div class="image-placeholder">
                <el-icon><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </div>

        <!-- 右侧信息 -->
        <div class="detail-info">
          <h1 class="detail-title">{{ activity.title || activity.name }}</h1>

          <div class="info-tags">
            <el-tag size="small">{{ activity.categoryName || activity.category || '其他' }}</el-tag>
            <el-tag
              v-if="refundPolicy === 0"
              type="danger"
              size="small"
            >不可退票</el-tag>
            <el-tag
              v-else
              type="success"
              size="small"
              effect="plain"
            >可退票</el-tag>
            <el-tag
              v-if="realNameMode === 2"
              type="warning"
              size="small"
            >一票一实名</el-tag>
            <el-tag
              v-else-if="realNameMode === 1"
              type="warning"
              size="small"
            >一单一实名</el-tag>
            <el-tag
              :type="getStatusType(getCurrentStatus())"
              size="small"
            >{{ getStatusText(getCurrentStatus()) }}</el-tag>
            <el-tag
              v-for="tag in activityTags"
              :key="tag.id"
              type="info"
              size="small"
              effect="plain"
            >{{ tag.name }}</el-tag>
          </div>

          <div class="info-row">
            <el-icon><Location /></el-icon>
            <span>{{ activity.venue || activity.location }}</span>
          </div>
          <div class="info-row">
            <el-icon><Clock /></el-icon>
            <span>{{ formatTime(activity.startTime) }} ~ {{ formatTime(activity.endTime) }}</span>
          </div>

          <div class="info-price">
            <span class="price-label">票价</span>
            <span class="price-value">¥{{ activity.price }}</span>
          </div>
          <div class="info-remain">
            剩余票数：<span class="remain-count">{{ activity.remainTickets }}</span> 张
          </div>
          <div class="info-reserve" v-if="activity.wishCount > 0">
            已有 <span class="reserve-count">{{ activity.wishCount }}</span> 人想看
          </div>

          <!-- 已售进度条：仅开售后展示（未开售活动显示"已售0张"观感矛盾） -->
          <div class="sold-progress" v-if="soldPercent !== null && getCurrentStatus() !== 1">
            <el-progress
              :percentage="soldPercent"
              :color="soldPercent > 80 ? '#f56c6c' : '#409EFF'"
              :stroke-width="10"
              :show-text="false"
            />
            <div class="sold-progress-text" :class="{ hot: soldPercent > 80 }">
              已售 {{ soldCount }} 张（{{ soldPercent }}%）
              <span v-if="soldPercent > 80" class="hot-flag">手慢无</span>
            </div>
          </div>

          <div class="action-area">
            <template v-if="getCurrentStatus() === 1">
              <el-button v-if="canReserve" type="warning" size="large" @click="handleReserve">
                <el-icon><Bell /></el-icon>{{ activity.reservationCount > 0 ? `预约提醒（${activity.reservationCount}人想看）` : '预约提醒' }}
              </el-button>
              <el-tag v-else type="info" size="large">本场未开放预约</el-tag>
              <div class="countdown" v-if="countdownText">
                <span class="countdown-label">距离开售</span>
                <span class="countdown-value">{{ countdownText }}</span>
              </div>
            </template>

            <template v-else-if="getCurrentStatus() === 2">
              <template v-if="activity.remainTickets > 0">
                <el-button type="danger" size="large" class="grab-btn" @click="openGrabDialog">
                  <el-icon><Lightning /></el-icon>立即抢票
                </el-button>
              </template>
              <template v-else>
                <el-button type="danger" size="large" class="grab-btn" disabled>
                  <el-icon><Lightning /></el-icon>已售罄
                </el-button>
                <el-tooltip content="到货提醒：售罄后登记，补票或重新开票时将通知你（与收藏独立）" placement="top">
                  <el-button
                    type="warning"
                    plain
                    size="large"
                    class="stock-notify-btn"
                    :loading="notifyLoading"
                    @click="handleNotify"
                  >
                    <el-icon><Bell /></el-icon>{{ notifyRegistered ? '已开到货提醒' : '到货提醒' }}
                  </el-button>
                </el-tooltip>
              </template>
              <div class="countdown selling" v-if="countdownText">
                <span class="countdown-label">距结束</span>
                <span class="countdown-value">{{ countdownText }}</span>
              </div>
            </template>

            <template v-else-if="getCurrentStatus() === 3">
              <el-tag type="info" size="large" class="ended-tag">已结束</el-tag>
            </template>

            <template v-else>
              <el-button v-if="canReserve" type="warning" size="large" @click="handleReserve">
                <el-icon><Bell /></el-icon>{{ activity.reservationCount > 0 ? `预约提醒（${activity.reservationCount}人想看）` : '预约提醒' }}
              </el-button>
              <el-tag v-else type="info" size="large">本场未开放预约</el-tag>
            </template>
          </div>

          <!-- 收藏 / 想看按钮（视觉区分：收藏=书签图标+琥珀色·私密；想看=星标+红色·公开热度） -->
          <div class="favorite-area">
            <el-tooltip content="收藏：存入「我的收藏」，仅自己可见" placement="top">
              <el-button
                :type="isFavorited ? 'warning' : 'default'"
                icon="Collection"
                @click="handleFavorite"
              >
                {{ isFavorited ? '已收藏' : '收藏' }}
              </el-button>
            </el-tooltip>
            <el-tooltip content="想看：表达兴趣并公开热度（显示「X 人想看」）" placement="top">
              <el-button
                :type="isWished ? 'danger' : 'default'"
                :icon="isWished ? 'StarFilled' : 'Star'"
                :loading="wishLoading"
                @click="handleWish"
              >
                {{ isWished ? '已想看' : '想看' }}
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </div>

      <!-- 观演温馨提示 / 场馆信息：聚合实用信息，让详情页更实用（对标大麦「观演须知」） -->
      <div class="detail-tips-card">
        <div class="tips-header">
          <span class="tips-icon">🎫</span>
          <span class="tips-title">观演温馨提示</span>
        </div>
        <div class="tips-grid">
          <div class="tip-item">
            <span class="tip-label">场馆</span>
            <span class="tip-value">{{ activity.venue || activity.location || '以现场为准' }}</span>
          </div>
          <div class="tip-item">
            <span class="tip-label">演出时间</span>
            <span class="tip-value">{{ formatTime(activity.startTime) }} ~ {{ formatTime(activity.endTime) }}</span>
          </div>
          <div class="tip-item">
            <span class="tip-label">入场时间</span>
            <span class="tip-value">演出前 60 分钟开始入场，请提前到达</span>
          </div>
          <div class="tip-item">
            <span class="tip-label">取票方式</span>
            <span class="tip-value">电子票，凭购票二维码或购票人身份证入场</span>
          </div>
          <div class="tip-item">
            <span class="tip-label">儿童政策</span>
            <span class="tip-value">1.2 米以下儿童免票（不占座），1.2 米以上凭票入场</span>
          </div>
          <div class="tip-item">
            <span class="tip-label">退票规则</span>
            <span class="tip-value" :class="{ 'tip-warn': refundPolicy === 0 }">{{ refundPolicy === 0 ? '不支持退票' : '支持退票（按规则收取手续费）' }}</span>
          </div>
          <div class="tip-item">
            <span class="tip-label">实名要求</span>
            <span class="tip-value">{{ realNameText }}</span>
          </div>
          <div class="tip-item tip-full">
            <span class="tip-label">禁带物品</span>
            <span class="tip-value">专业摄录设备、大功率充电宝、易燃易爆及管制物品等请勿携带入场</span>
          </div>
        </div>
      </div>

      <!-- 演出阵容 / 嘉宾：运营后台可配置的真实阵容（对标大麦「艺人阵容」），无数据则不展示 -->
      <div class="lineup-section" v-if="lineupList.length > 0">
        <div class="section-header">
          <h2 class="section-title">🎤 演出阵容 / 嘉宾</h2>
          <span class="reco-tip">{{ lineupList.length }} 位主要阵容</span>
        </div>
        <div class="lineup-grid">
          <div
            v-for="g in lineupList"
            :key="'lu-' + g.id"
            class="lineup-card"
          >
            <el-avatar :size="64" :src="g.avatar" class="lineup-avatar">
              {{ g.name ? g.name.charAt(0) : '?' }}
            </el-avatar>
            <div class="lineup-name">{{ g.name }}</div>
            <div class="lineup-role" v-if="g.role">{{ g.role }}</div>
            <div class="lineup-desc" v-if="g.description">{{ g.description }}</div>
          </div>
        </div>
      </div>

      <!-- 下方详情Tab -->
      <div class="detail-tabs">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="活动介绍" name="intro">
            <!-- 基本信息卡：用真实字段快速了解活动（对标大麦「演出信息」） -->
            <div class="intro-basic" v-if="activity">
              <div class="ib-item">
                <span class="ib-label">演出时间</span>
                <span class="ib-value">{{ formatTime(activity.startTime) }}</span>
              </div>
              <div class="ib-item">
                <span class="ib-label">演出场馆</span>
                <span class="ib-value">{{ activity.venue || '以现场为准' }}</span>
              </div>
              <div class="ib-item">
                <span class="ib-label">活动分类</span>
                <span class="ib-value">{{ categoryNameText }}</span>
              </div>
              <div class="ib-item">
                <span class="ib-label">演出时长</span>
                <span class="ib-value">{{ durationText }}</span>
              </div>
              <div class="ib-item">
                <span class="ib-label">座位方式</span>
                <span class="ib-value">{{ seatMapText }}</span>
              </div>
              <div class="ib-item">
                <span class="ib-label">活动热度</span>
                <span class="ib-value ib-hot">❤ {{ activity.wishCount || 0 }} 人想看 · 👁 {{ activity.viewCount || 0 }} 浏览</span>
              </div>
            </div>
            <!-- 活动详情正文 -->
            <div class="tab-content intro-desc" v-html="activity.description || '暂无介绍'"></div>
            <!-- 场馆位置卡（无地图 API，用样式化定位卡片表达「场馆在哪」） -->
            <div class="intro-venue" v-if="activity.venue">
              <div class="iv-map">
                <div class="iv-pin">📍</div>
                <div class="iv-map-label">场馆位置</div>
              </div>
              <div class="iv-info">
                <div class="iv-name">{{ activity.venue }}</div>
                <div class="iv-tip">建议优先选择公共交通前往；具体路线与出入口以现场指引及导航为准。请于演出前 60 分钟到达，预留安检与取票时间。</div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="票务规则" name="rule">
            <div class="tab-content" v-html="activity.ticketRule || '暂无票务规则'"></div>
          </el-tab-pane>
          <el-tab-pane label="限购说明" name="limit">
            <div class="tab-content">
              <p>每人限购 <strong>{{ activity.purchaseLimit || 1 }}</strong> 张</p>
              <p>每个账号仅可购买一次，购买后不可转让</p>
              <p v-if="realNameMode === 2">本场为「一票一实名」，每张门票必须填写一位观演人身份证</p>
              <p v-else-if="realNameMode === 1">本场为「一单一实名」，填写 1 位实名购票人即可购买多张门票</p>
            </div>
          </el-tab-pane>
          <el-tab-pane label="购票须知/注意事项" name="notice">
            <div class="tab-content">
              <ul class="notice-list">
                <li>
                  退票规则：<strong :class="refundPolicy === 0 ? 'notice-no' : 'notice-yes'">{{ refundableText }}</strong>
                </li>
                <li>实名要求：{{ realNameText }}</li>
                <li>座位定位：{{ seatMapText }}</li>
                <li>购票限制：每人限购 {{ activity.purchaseLimit || 1 }} 张，购买后不可转让</li>
              </ul>
              <div v-if="activity.ticketNotice" class="notice-extra">
                <div class="notice-extra-title">活动注意事项</div>
                <div class="notice-extra-body"><span v-html="activity.ticketNotice"></span></div>
              </div>
              <div v-if="activity.ticketRule" class="notice-extra">
                <div class="notice-extra-title">票务规则补充</div>
                <div class="notice-extra-body"><span v-html="activity.ticketRule"></span></div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 评价区域 -->
      <div class="review-section">
        <div class="review-header">
          <span class="section-title">用户评价</span>
          <el-button
            v-if="canReview"
            type="primary"
            :icon="EditPen"
            @click="openReviewDialog"
          >发布评价</el-button>
        </div>

        <!-- 评价统计 -->
        <div class="review-statistics" v-loading="reviewLoading">
          <div class="stat-left">
            <div class="avg-score">{{ avgScore }}</div>
            <el-rate
              :model-value="Number(avgScore)"
              disabled
              show-score
              :score-template="`${avgScore} 分`"
              class="avg-rate"
            />
            <div class="total-count">共 {{ reviewStatistics.totalCount || 0 }} 条评价</div>
          </div>
          <div class="stat-right">
            <div
              v-for="star in [5, 4, 3, 2, 1]"
              :key="star"
              class="star-row"
            >
              <span class="star-label">{{ star }}星</span>
              <el-progress
                :percentage="getStarPercentage(star)"
                :stroke-width="12"
                :show-text="true"
                :format="() => `${getStarCount(star)} 条`"
                class="star-progress"
              />
            </div>
          </div>
        </div>

        <!-- 评价列表 -->
        <div class="review-list" v-loading="reviewLoading">
          <div v-if="reviews.length === 0 && !reviewLoading" class="review-empty">
            <el-empty description="暂无评价，快来抢沙发吧" :image-size="80" />
          </div>
          <div
            v-for="review in reviews"
            :key="review.id"
            class="review-item"
          >
            <div class="review-avatar">
              <el-avatar :size="44" :src="review.avatar">
                {{ (review.nickname || '用户').charAt(0) }}
              </el-avatar>
            </div>
            <div class="review-main">
              <div class="review-top">
                <span class="review-nickname">{{ review.nickname || '匿名用户' }}</span>
                <el-rate
                  :model-value="Number(review.score || review.rating || 0)"
                  disabled
                  size="small"
                />
                <span class="review-time">{{ formatTime(review.createTime) }}</span>
              </div>
              <div class="review-content">{{ review.content || review.comment || '' }}</div>
              <div class="review-images" v-if="parseImages(review.images).length">
                <el-image
                  v-for="(img, idx) in parseImages(review.images)"
                  :key="idx"
                  :src="img"
                  :preview-src-list="parseImages(review.images)"
                  :initial-index="idx"
                  class="review-image"
                  fit="cover"
                  preview-teleported
                  lazy
                >
                  <template #error>
                    <div class="review-image review-image-error">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
              <!-- 商家回复 -->
              <div class="review-reply" v-if="review.reply || review.replyContent">
                <div class="reply-tag">商家回复：</div>
                <div class="reply-content">{{ review.reply || review.replyContent }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 相关推荐：同分类在售活动，复用推荐能力，提升连带浏览（对标大麦「看了又看」） -->
      <div class="related-section" v-if="relatedList.length > 0">
        <div class="section-header">
          <h2 class="section-title">🎯 相关推荐</h2>
          <span class="reco-tip">更多同类型精彩演出</span>
        </div>
        <el-row :gutter="20">
          <el-col
            v-for="item in relatedList"
            :key="'rel-' + item.id"
            :xs="24" :sm="12" :md="8" :lg="6"
          >
            <div class="related-card" @click="goRelated(item.id)">
              <div class="related-cover-wrap">
                <el-image
                  class="related-cover"
                  :src="item.coverImage"
                  fit="cover"
                  lazy
                >
                  <template #error>
                    <div class="image-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
              <div class="related-info">
                <h3 class="related-title">{{ item.title || item.name }}</h3>
                <p class="related-location"><el-icon><Location /></el-icon>{{ item.venue || item.location }}</p>
                <div class="related-bottom">
                  <span class="related-price">¥{{ item.price }}</span>
                  <span class="related-remain">剩余 {{ item.remainTickets != null ? item.remainTickets : Math.max(0, Number(item.totalTickets || 0) - Number(item.soldTickets || 0)) }} 张</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>

    <el-empty v-if="!activity && !loading" description="活动不存在" />

    <!-- 发布评价对话框 -->
    <el-dialog
      v-model="reviewDialogVisible"
      title="发布评价"
      width="560px"
      destroy-on-close
    >
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="活动名称">
          <span class="review-activity-name">{{ activity?.title || activity?.name }}</span>
        </el-form-item>
        <el-form-item label="总体评分">
          <el-rate
            v-model="reviewForm.score"
            :max="5"
            show-text
            :texts="['很差', '较差', '一般', '较好', '很好']"
          />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请分享您的观演体验（最多500字）"
          />
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload
            v-model:file-list="reviewForm.fileList"
            action="/api/upload"
            list-type="picture-card"
            :auto-upload="true"
            :limit="6"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-exceed="handleUploadExceed"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">最多上传 6 张图片，每张不超过 5MB；图片会自动上传至服务器</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitReviewLoading"
          @click="submitReview"
        >发布评价</el-button>
      </template>
    </el-dialog>

    <!-- 预约确认弹窗 -->
    <el-dialog v-model="showReserveDialog" title="预约提醒" width="460px" align-center>
      <p>确认预约该活动的抢票提醒？</p>
      <div class="reserve-crowd-tip" v-if="activity.wishCount > 0">
        已有 <b>{{ activity.wishCount }}</b> 人想看，竞争激烈，开售后将按预约顺序优先出票！
      </div>
      <!-- 预约选定价位：开抢后直接按所选价位出票 -->
      <div class="reserve-seat-section" v-if="reservationTicketPrices.length">
        <div class="reserve-seat-title">选择想要的价位（开抢后按该价位直接出票）</div>
        <div class="reserve-seat-list">
          <div
            v-for="tp in reservationTicketPrices"
            :key="tp.id"
            class="reserve-seat-card"
            :class="{ selected: reservedPriceId === tp.id }"
            @click="selectReservedPrice(tp)"
          >
            <span class="reserve-seat-name">{{ tp.seatType || '普通票' }}</span>
            <span class="reserve-seat-price">¥{{ Number(tp.price || 0).toFixed(2) }}</span>
          </div>
        </div>
      </div>
      <div class="reserve-seat-note" v-else>
        本场未设置分档票价，开抢后将按默认票价出票
      </div>
      <p style="color: #909399; font-size: 13px; margin-top: 12px;">
        开售后系统将优先为您出票，并全程播放<span v-if="countdownText">距离开售倒计时：<b style="color:#e6a23c">{{ countdownText }}</b></span>；开售前会通过站内消息提醒您，请提前进入页面等待。
      </p>
      <template #footer>
        <el-button @click="showReserveDialog = false">取消</el-button>
        <el-button type="warning" :loading="actionLoading" @click="confirmReserve">确认预约</el-button>
      </template>
    </el-dialog>

    <!-- 抢票确认弹窗 -->
    <el-dialog
      v-model="showGrabDialog"
      title="确认订单信息"
      width="560px"
      align-center
      destroy-on-close
      class="grab-dialog"
    >
      <div class="grab-dialog-body">
        <div class="grab-activity-name">{{ activity?.title || activity?.name }}</div>

        <!-- 票档选择 -->
        <div class="grab-section">
          <div class="grab-section-title">选择票档</div>
          <div v-if="ticketPriceLoading" class="ticket-price-loading">
            <el-skeleton :rows="2" animated />
          </div>
          <template v-else>
            <div class="ticket-price-list" v-if="ticketPrices.length">
              <div
                v-for="tp in ticketPrices"
                :key="tp.id"
                class="ticket-price-card"
                :class="{
                  selected: selectedPriceId === tp.id,
                  disabled: getTicketRemain(tp) <= 0
                }"
                @click="selectTicketPrice(tp)"
              >
                <div class="tp-left">
                  <div class="tp-name">{{ tp.seatType || '普通票' }}</div>
                  <div
                    class="tp-remain"
                    :class="{
                      warn: getTicketRemain(tp) > 0 && getTicketRemain(tp) < 50,
                      out: getTicketRemain(tp) <= 0
                    }"
                  >
                    <template v-if="getTicketRemain(tp) <= 0">售罄</template>
                    <template v-else-if="getTicketRemain(tp) < 50">仅剩 {{ getTicketRemain(tp) }} 张</template>
                    <template v-else>余票 {{ getTicketRemain(tp) }} 张</template>
                  </div>
                </div>
                <div class="tp-right">
                  <span class="tp-price">¥{{ Number(tp.price || 0).toFixed(2) }}</span>
                </div>
                <div class="tp-check" v-if="selectedPriceId === tp.id">
                  <el-icon><Check /></el-icon>
                </div>
              </div>
            </div>
            <!-- 无票档数据时退回统一票价 -->
            <div class="ticket-price-list" v-else>
              <div class="ticket-price-card selected">
                <div class="tp-left">
                  <div class="tp-name">统一票价</div>
                  <div class="tp-remain">余票 {{ activity?.remainTickets ?? '-' }} 张</div>
                </div>
                <div class="tp-right">
                  <span class="tp-price">¥{{ Number(activity?.price || 0).toFixed(2) }}</span>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 购票数量 -->
        <div class="grab-section">
          <div class="grab-section-title">购票数量</div>
          <div class="quantity-row">
            <el-input-number
              v-model="grabCount"
              :min="1"
              :max="maxGrabCount"
            />
            <span class="limit-tip">每单限购 {{ activity?.purchaseLimit || 1 }} 张</span>
          </div>
        </div>

        <!-- 在线选座（仅开启场馆地图的选座场次显示，对标大麦「在线选座」） -->
        <div class="grab-section" v-if="seatMapEnabled === 1">
          <div class="grab-section-title seat-title">
            在线选座<span class="section-sub">（{{ selectedTicketPrice ? selectedTicketPrice.seatType + ' · ' : '' }}已选 {{ selectedSeats.length }}/{{ grabCount }} 个，座位数须与票数一致）</span>
            <el-button
              v-if="view3dEnabled"
              size="small"
              type="primary"
              plain
              class="seat-3d-btn"
              @click="openVenue3D"
            >🎫 3D 视野预览</el-button>
          </div>
          <div v-if="seatLoading" class="seat-loading">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="parsedSeatMap" class="seat-map">
            <div class="seat-legend">
              <span class="legend-item"><i class="seat-dot available"></i>可选</span>
              <span class="legend-item"><i class="seat-dot selected"></i>已选</span>
              <span class="legend-item"><i class="seat-dot sold"></i>已售</span>
              <span class="legend-item"><i class="seat-dot locked"></i>锁定</span>
            </div>
            <div v-for="sec in visibleSeatSections" :key="sec.id" class="seat-section">
              <div class="seat-section-name">
                {{ sec.name }}<span v-if="sec.price" class="seat-section-price">¥{{ Number(sec.price).toFixed(2) }}</span>
              </div>
              <div class="seat-grid">
                <div v-for="row in sec.seatRows" :key="row.row" class="seat-row">
                  <span class="seat-row-label">{{ row.row }}排</span>
                  <div class="seat-row-seats">
                    <button
                      v-for="seat in row.seats"
                      :key="seat.code"
                      type="button"
                      class="seat-cell"
                      :class="seat.status"
                      :disabled="seat.status === 'sold' || seat.status === 'locked'"
                      @click="toggleSeat(seat)"
                    >{{ seat.col }}</button>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="selectedSeats.length" class="selected-seats">
              已选座位：
              <span v-for="code in selectedSeats" :key="code" class="selected-seat-tag">{{ code }}</span>
            </div>
          </div>
          <div v-else class="seat-empty">暂无座位信息，请稍后重试</div>
        </div>

        <!-- 场馆视野预览（未开启在线选座、座位由系统统一安排的场次） -->
        <div class="grab-section" v-if="view3dEnabled && seatMapEnabled !== 1">
          <div class="grab-section-title">
            场馆视野<span class="section-sub">（本场次不开放在线选座，座位由系统按票档统一安排）</span>
            <el-button
              size="small"
              type="primary"
              plain
              class="seat-3d-btn"
              @click="openVenue3D"
            >🏟️ 3D 场馆视野</el-button>
          </div>
        </div>

        <!-- 3D 场馆视野预览（仅开启 3D 的选座场次显示，对标大麦「选座看视野」） -->
        <el-dialog
          v-model="venue3dVisible"
          :title="`3D 场馆视野预览 · ${venueModelLabel}`"
          width="780px"
          append-to-body
          top="5vh"
          class="venue3d-dialog"
        >
          <div v-if="seatMapEnabled !== 1" class="venue3d-assign-tip">
            本场次座位由系统统一安排，以下为场馆布局与各区域参考视野；购买对应票档后将分配同区域座位。
          </div>
          <div style="height: 480px;">
            <Venue3DViewer
              v-if="venue3dVisible"
              :sections="parsedSeatMap ? parsedSeatMap.sections : []"
              :selected-seat-code="lastSelectedSeat"
              :occupied="seatOccupied"
              :model="activity ? (activity.view3dModel || 'arena_bowl') : 'arena_bowl'"
            />
          </div>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="venue3dVisible = false">关闭</el-button>
            </span>
          </template>
        </el-dialog>

        <!-- 实名观演人：数量与文案随活动实名模式变化 -->
        <div class="grab-section">
          <div class="grab-section-title">
            实名观演人<span class="section-sub">（{{ attendeeModeTip }}）</span>
          </div>
          <div class="attendee-tip">{{ attendeeTipText }}</div>
          <div
            v-for="(slot, idx) in grabAttendeeList"
            :key="idx"
            class="attendee-slot"
          >
            <div class="attendee-slot-head">
              <span class="attendee-slot-label">{{ realNameMode === 1 ? '实名购票人' : `第 ${idx + 1} 张` }}</span>
              <el-select
                v-if="userStore.isLoggedIn && attendees.length"
                v-model="slot.attendeeId"
                placeholder="选择常用观演人"
                clearable
                class="attendee-slot-select"
                @change="handleAttendeeSelect(idx, $event)"
              >
                <el-option
                  v-for="a in attendees"
                  :key="a.id"
                  :label="`${a.name}（${maskIdCard(a.idCard)}）`"
                  :value="a.id"
                />
              </el-select>
            </div>
            <div class="attendee-inputs">
              <el-input v-model="slot.attendeeName" placeholder="观演人姓名" clearable maxlength="20" />
              <el-input v-model="slot.attendeeIdCard" placeholder="身份证号（18 位）" clearable maxlength="18" />
            </div>
          </div>
          <div v-if="!grabAttendeeList.length" class="attendee-empty">请先选择购票数量</div>
        </div>

        <!-- 金额明细 -->
        <div class="amount-detail">
          <div class="amount-line">
            <span>票价单价</span>
            <span>¥{{ unitPrice.toFixed(2) }}</span>
          </div>
          <div class="amount-line">
            <span>数量</span>
            <span>× {{ grabCount }}</span>
          </div>
          <div class="amount-line total">
            <span>应付金额</span>
            <span class="amount-total">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
        </div>

        <div
          class="agreement-tip"
          :class="{ 'agreement-no': refundPolicy === 0 }"
        >{{ refundPolicy === 0 ? '本场为「不可退票」，提交即视为同意《购票须知》' : '提交即视为同意《购票须知》与《退换票规则》' }}</div>
      </div>
      <template #footer>
        <el-button @click="showGrabDialog = false">取 消</el-button>
        <el-button
          type="danger"
          :loading="actionLoading"
          :disabled="submittingGrab || (activity && activity.remainTickets <= 0)"
          @click="confirmGrab"
        >确认抢票</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Plus, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { getActivityDetail, getActivityList, toggleWish, getMyWish, toggleNotify, getMyNotify, getActivityLineup } from '@/api/activity'
import { applyReservation, cancelReservation, getReservationDetail } from '@/api/reservation'
import { grabTicket } from '@/api/ticket'
import { getSeatMap } from '@/api/seat'
import { getAttendeeList } from '@/api/attendee'
import { addFavorite, removeFavorite, getMyFavorites } from '@/api/favorite'
import { getMyOrders, payOrder, cancelOrder } from '@/api/order'
import { addReview, getPublicReviews, getReviewStatistics } from '@/api/review'
import { getActivityTags, getEnabledTags } from '@/api/tag'
import Venue3DViewer from '@/components/Venue3DViewer.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const actionLoading = ref(false)
const submittingGrab = ref(false)
const activity = ref(null)
const isFavorited = ref(false)
// 想看（对标大麦“X人想看”）
const isWished = ref(false)
const wishLoading = ref(false)
const notifyRegistered = ref(false)
const notifyLoading = ref(false)
const activeTab = ref('intro')
const showReserveDialog = ref(false)
const showGrabDialog = ref(false)
const grabCount = ref(1)
const countdownText = ref('')
const activityTags = ref([])
// 待支付订单相关
const pendingOrder = ref(null)
const pendingOrderCountdown = ref('')
let pendingOrderTimer = null
let countdownTimer = null

// ========== 抢票弹窗相关状态 ==========
const ticketPrices = ref([])
const ticketPriceLoading = ref(false)
const selectedPriceId = ref(null)
const attendees = ref([])
// 预约弹窗票档选择：预约时选定价位，开抢后按该价位直接出票
const reservationTicketPrices = ref([])
const reservedPriceId = ref(null)
// 抢票弹窗内动态观演人槽位：数量随购票数量联动，每张票绑定一位观演人
const grabAttendeeList = ref([{ attendeeName: '', attendeeIdCard: '', attendeeId: null }])

// 在线选座（对标大麦「在线选座」）：选座场次专用
const seatMap = ref(null)        // 后端返回原始座位图 { seatMapEnabled, layout, occupied, locked }
const seatLoading = ref(false)
const selectedSeats = ref([])    // 已选座位编码数组，如 ["A-3-5","A-3-6"]

// 3D 场馆视野预览
const venue3dVisible = ref(false)
const lastSelectedSeat = ref('')   // 最近一次选中的座位（用于「从座位看」默认视角）
const seatOccupied = computed(() => (seatMap.value && seatMap.value.occupied) || [])
const openVenue3D = () => { venue3dVisible.value = true }

// 票档余票 = totalStock - soldStock
const getTicketRemain = (tp) => {
  if (!tp) return 0
  return Number(tp.totalStock ?? 0) - Number(tp.soldStock ?? 0)
}

const selectedTicketPrice = computed(() => {
  return ticketPrices.value.find(tp => tp.id === selectedPriceId.value) || null
})

// 单价：优先所选票档价格，未选票档时退回 activity.price
const unitPrice = computed(() => {
  if (selectedTicketPrice.value) return Number(selectedTicketPrice.value.price || 0)
  return Number(activity.value?.price || 0)
})

const totalPrice = computed(() => {
  return (unitPrice.value || 0) * (grabCount.value || 0)
})

// 数量上限 = min(限购, 所选票档余票, 活动余票)
const maxGrabCount = computed(() => {
  const limit = Number(activity.value?.purchaseLimit || 1)
  const stock = selectedTicketPrice.value
    ? Math.max(getTicketRemain(selectedTicketPrice.value), 0)
    : Number(activity.value?.remainTickets ?? limit)
  return Math.max(Math.min(limit, stock || 1), 1)
})

// 已售张数 & 百分比
const soldCount = computed(() => Number(activity.value?.soldTickets || 0))
const soldPercent = computed(() => {
  const total = Number(activity.value?.totalTickets || 0)
  const sold = soldCount.value
  if (!total) return null
  const p = (sold / total) * 100
  // 小于 10% 时保留 1 位小数，避免像 30/8000 这种被四舍五入成 0%
  const rounded = p < 10 ? Math.round(p * 10) / 10 : Math.round(p)
  return Math.min(rounded, 100)
})

// 数量超出上限时自动收敛
watch(() => maxGrabCount.value, (val) => {
  if (grabCount.value > val) grabCount.value = val
})

// 观演人槽位数：
// 一单一实名(1) → 只展示 1 位“实名购票人”，可购买多张
// 一票一实名(2) → 购几张票就展示几个实名位，每张都必须填写
// 免实名(0) → 购几张票展示几个可选的观演人位，全部留空为匿名
const syncGrabAttendeeSlots = (n) => {
  const mode = realNameMode.value
  const count = mode === 1 ? 1 : Math.max(1, Number(n) || 1)
  const next = []
  for (let i = 0; i < count; i++) {
    const src = grabAttendeeList.value[i]
    next.push({
      attendeeName: src && src.attendeeName ? src.attendeeName : '',
      attendeeIdCard: src && src.attendeeIdCard ? src.attendeeIdCard : '',
      attendeeId: src && src.attendeeId != null ? src.attendeeId : null
    })
  }
  grabAttendeeList.value = next
}
watch(grabCount, (val) => {
  syncGrabAttendeeSlots(val)
  // 选座场次：已选座位数不得超过购票数量，超出则裁剪
  if (selectedSeats.value.length > val) {
    selectedSeats.value = selectedSeats.value.slice(0, val)
  }
})

// ========== 评价相关状态 ==========
const reviewLoading = ref(false)
const reviews = ref([])
const reviewStatistics = ref({})
const reviewDialogVisible = ref(false)
const submitReviewLoading = ref(false)
const reviewForm = ref({
  score: 5,
  content: '',
  fileList: []
})
// 评价图片上传所需的鉴权头（el-upload 直接走 XHR，不经过 axios 拦截器，需手动带 token）
const uploadHeaders = { Authorization: 'Bearer ' + (localStorage.getItem('token') || '') }
const handleUploadSuccess = (response, file) => {
  if (response && response.code === 200 && response.data) {
    file.url = response.data
  } else {
    ElMessage.error((response && response.message) || '图片上传失败')
  }
}
// 当前用户是否已购买该活动门票（用于判断是否可评价）
const hasPurchased = ref(false)

const canReview = computed(() => {
  return userStore.isLoggedIn && hasPurchased.value
})

const avgScore = computed(() => {
  const score = Number(reviewStatistics.value?.avgRating || 0)
  return score.toFixed(1)
})

// 活动ID快照：直接用 computed(() => route.params.id) 时，一旦离开本页（如支付成功后跳转订单页），
// params.id 会变成 undefined；若此时仍有异步请求在飞，就会拼出 /activity/detail/undefined 这类地址，
// 后端 Long 转换失败并返回“系统内部错误”。这里改成只在新路由确实带 id 时更新，避免参数被清空。
const activityId = ref(route.params.id)
watch(
  () => route.params.id,
  (id) => {
    if (id !== undefined && id !== null && id !== '') {
      activityId.value = id
    }
  }
)

// ========== 活动票务规则（退票/实名/场馆地图） ==========
const refundPolicy = computed(() => Number(activity.value?.refundPolicy ?? 1))
const realNameMode = computed(() => Number(activity.value?.realNameMode ?? 0))
const seatMapEnabled = computed(() => Number(activity.value?.seatMapEnabled ?? 0))
const view3dEnabled = computed(() => Number(activity.value?.view3dEnabled ?? 0) === 1)
// 3D 场馆模型中文名（与运营端编辑页选项保持一致）
const VENUE_MODEL_LABELS = {
  arena_bowl: '碗形综合馆',
  theatre: '镜框式剧院',
  stadium: '标准体育场',
  concert: '露天音乐场'
}
const venueModelLabel = computed(() => VENUE_MODEL_LABELS[activity.value?.view3dModel] || VENUE_MODEL_LABELS.arena_bowl)
const reservationEnabled = computed(() => Number(activity.value?.reservationEnabled ?? 1))

// 将后端返回的座位布局 JSON 解析为可渲染的分区/排/座结构；状态：available/selected/sold/locked
const parsedSeatMap = computed(() => {
  if (!seatMap.value || !seatMap.value.layout) return null
  try {
    const layout = typeof seatMap.value.layout === 'string'
      ? JSON.parse(seatMap.value.layout)
      : seatMap.value.layout
    const occupied = seatMap.value.occupied || []
    const locked = seatMap.value.locked || []
    const sections = (layout.sections || []).map(sec => {
      const rows = Number(sec.rows) || 0
      const cols = Number(sec.cols) || 0
      const seatRows = []
      for (let r = 1; r <= rows; r++) {
        const rowSeats = []
        for (let c = 1; c <= cols; c++) {
          const code = `${sec.id}-${r}-${c}`
          let status = 'available'
          if (occupied.includes(code)) status = 'sold'
          else if (locked.includes(code)) status = 'locked'
          else if (selectedSeats.value.includes(code)) status = 'selected'
          rowSeats.push({ code, row: r, col: c, status })
        }
        seatRows.push({ row: r, seats: rowSeats })
      }
      return { id: sec.id, name: sec.name || sec.id, price: sec.price, rows, cols, seatRows }
    })
    return { sections }
  } catch (e) {
    return null
  }
})

// 与所选票档匹配的座位分区：优先分区名 === 票档名，其次按票价匹配；未选票档时显示全部分区
const visibleSeatSections = computed(() => {
  const all = parsedSeatMap.value ? parsedSeatMap.value.sections : []
  const tier = selectedTicketPrice.value
  if (!tier || !all.length) return all
  const tierName = String(tier.seatType || '').trim()
  const byName = tierName ? all.filter(s => String(s.name).trim() === tierName) : []
  if (byName.length) return byName
  const byPrice = all.filter(s => Number(s.price) === Number(tier.price))
  if (byPrice.length) return byPrice
  return all
})

// 切换票档时清空已选座位（不同票档分区不同，避免脏选择）
watch(selectedPriceId, () => {
  if (selectedSeats.value.length) selectedSeats.value = []
  lastSelectedSeat.value = ''
})

const refundableText = computed(() =>
  refundPolicy.value === 0 ? '购票后不可退票（请购票前仔细确认）' : '支持退票（按退票规则收取一定手续费，以审核结果为准）'
)
const realNameText = computed(() => {
  const m = realNameMode.value
  if (m === 2) return '一票一实名：每张门票必须填写一位观演人身份证，一张身份证限购一张'
  if (m === 1) return '一单一实名：只需填写 1 位实名购票人身份证，即可购买多张门票（需本人+登录实名一致）'
  return '免实名：本场不强制实名，购票时可不填观演人信息'
})
const seatMapText = computed(() =>
  seatMapEnabled.value === 1 ? '本场已开通场馆地图，购票后可在电子票中直接定位到对应座位' : '本场未提供座位定位，请按票面区域进场'
)
// 活动分类名称（categoryName 后端多为空，用本地映射兜底）
const categoryNameMap = { 1: '演唱会', 2: '话剧演出', 3: '艺术展览', 4: '体育赛事', 5: '校园文娱' }
const categoryNameText = computed(() =>
  categoryNameMap[activity.value?.categoryId] || activity.value?.categoryName || '其他'
)
// 演出时长（按开始/结束时间推算，真实数据）
const durationText = computed(() => {
  const s = activity.value?.startTime
  const e = activity.value?.endTime
  if (!s || !e) return '以现场安排为准'
  const start = new Date(s)
  const end = new Date(e)
  const ms = end.getTime() - start.getTime()
  if (isNaN(ms) || ms <= 0) return '以现场安排为准'
  const mins = Math.round(ms / 60000)
  if (mins < 60) return `${mins} 分钟`
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return m ? `${h} 小时 ${m} 分钟` : `${h} 小时`
})
const canReserve = computed(() => reservationEnabled.value === 1)

const attendeeModeTip = computed(() => {
  const m = realNameMode.value
  if (m === 2) return `一票一实名，共 ${grabCount.value} 张`
  if (m === 1) return '一单一实名，可购多张'
  return `可选，共 ${grabCount.value} 张`
})
const attendeeTipText = computed(() => {
  const m = realNameMode.value
  if (m === 2) return '每张票都必须填写一位观演人身份证；同一身份证本场限购一张'
  if (m === 1) return '只需填写 1 位实名购票人身份证，即可购买多张门票，入场凭电子票验证码核销'
  return '每张票可填一位观演人，全部留空则按匿名购买'
})

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const getCurrentStatus = () => {
  if (!activity.value) return 3
  const now = new Date().getTime()
  const grabStart = new Date(activity.value.grabStartTime).getTime()
  const grabEnd = new Date(activity.value.grabEndTime).getTime()
  
  if (isNaN(grabStart) || isNaN(grabEnd)) {
    return activity.value.status || 3
  }
  
  if (now < grabStart) return 1
  if (now >= grabStart && now <= grabEnd) return 2
  return 3
}

const getStatusType = (status) => {
  const map = { 0: 'info', 1: 'warning', 2: 'danger', 3: 'info' }
  return map[status] ?? 'info'
}

const getStatusText = (status) => {
  const map = { 0: '未开启', 1: '预约中', 2: '抢票中', 3: '已结束' }
  return map[status] ?? '未知'
}

const updateCountdown = () => {
  if (!activity.value) {
    countdownText.value = ''
    return
  }
  const status = getCurrentStatus()
  // 抢票未开始：倒计时到 grabStartTime；抢票中：倒计时到 grabEndTime
  const targetTimeStr = status === 2 ? activity.value.grabEndTime : activity.value.grabStartTime
  if (!targetTimeStr) {
    countdownText.value = ''
    return
  }
  const now = new Date().getTime()
  const target = new Date(targetTimeStr).getTime()
  if (isNaN(target)) {
    countdownText.value = ''
    return
  }
  const diff = target - now
  if (diff <= 0) {
    countdownText.value = status === 2 ? '已结束' : '即将开始'
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
    return
  }
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)
  const pad = (n) => String(n).padStart(2, '0')
  countdownText.value = `${days > 0 ? days + '天' : ''}${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
}

const startCountdown = () => {
  if (countdownTimer) clearInterval(countdownTimer)
  updateCountdown()
  countdownTimer = setInterval(updateCountdown, 1000)
}

const fetchDetail = async () => {
  // 兜底：没有有效活动ID时不发请求，避免把 "undefined" 拼进 URL
  if (!activityId.value) {
    loading.value = false
    return
  }
  loading.value = true
  try {
    const res = await getActivityDetail(activityId.value)
    activity.value = res.data
    // 如果抢票尚未开始或正在抢票中，启动倒计时
    if (getCurrentStatus() === 1 || getCurrentStatus() === 2) {
      startCountdown()
    }
    // 查询预约和收藏状态
    await checkReservationStatus()
    await checkFavoriteStatus()
    // 查询到货提醒状态（独立，不再与收藏耦合）
    await checkNotifyStatus()
    // 查询想看状态
    await checkWishStatus()
    // 查询评价和购票状态
    await fetchReviews()
    await fetchReviewStatistics()
    await checkPurchaseStatus()
    // 查询待支付订单
    await checkPendingOrder()
    // 查询活动标签
    await fetchActivityTags()
    // 查询同分类相关推荐（提升连带浏览，对标大麦「看了又看」）
    await fetchRelated()
    // 查询演出阵容 / 嘉宾（运营后台可配置）
    await fetchLineup()
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

const fetchActivityTags = async () => {
  try {
    const tagIds = await getActivityTags(activityId.value)
    const ids = tagIds.data || []
    if (ids.length === 0) {
      activityTags.value = []
      return
    }
    // 获取所有启用标签，然后筛选当前活动的标签
    const allTagsRes = await getEnabledTags()
    const allTags = allTagsRes.data || []
    activityTags.value = allTags.filter(tag => ids.includes(tag.id))
  } catch {
    activityTags.value = []
  }
}

// 演出阵容 / 嘉宾（运营后台可配置，详情页展示）
const lineupList = ref([])
const fetchLineup = async () => {
  try {
    const res = await getActivityLineup(activityId.value)
    lineupList.value = Array.isArray(res.data) ? res.data : []
  } catch {
    lineupList.value = []
  }
}

// 相关推荐：同分类在售活动（排除当前），复用列表接口，无需登录
const relatedList = ref([])
const fetchRelated = async () => {
  const catId = activity.value?.categoryId
  if (!catId) return
  try {
    const res = await getActivityList({ categoryId: catId, page: 1, size: 7 })
    const list = res.data?.records || res.data?.list || []
    relatedList.value = list
      .filter(it => Number(it.id) !== Number(activityId.value))
      .slice(0, 6)
  } catch {
    relatedList.value = []
  }
}

// 跳转到相关活动详情
const goRelated = (id) => {
  router.push(`/activity/${id}`)
}

const checkReservationStatus = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getReservationDetail(activityId.value)
    // data 为 null 表示未预约，属正常返回，静默处理
    const detail = res?.data
    if (!detail) return
    // 已预约，不做特殊处理，仅标记
  } catch {
    // 未预约或查询失败，静默处理
  }
}

const checkFavoriteStatus = async () => {
  if (!userStore.isLoggedIn) return
  try {
    const res = await getMyFavorites({ page: 1, size: 1000 })
    const list = res.data?.records || res.data?.list || []
    isFavorited.value = list.some(item => String(item.activityId) === String(activityId.value))
  } catch {
    // ignore
  }
}

// 查询当前用户是否已开启到货提醒（独立于收藏）
const checkNotifyStatus = async () => {
  try {
    const res = await getMyNotify(activityId.value)
    const data = res?.data || {}
    notifyRegistered.value = !!data.notified
  } catch {
    notifyRegistered.value = false
  }
}

// 查询当前用户是否已想看
const checkWishStatus = async () => {
  try {
    const res = await getMyWish(activityId.value)
    const data = res?.data || {}
    isWished.value = !!data.wished
  } catch {
    isWished.value = false
  }
}

// 想看 / 取消想看（对标大麦“X人想看”）
const handleWish = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (wishLoading.value) return
  wishLoading.value = true
  try {
    const res = await toggleWish(activityId.value)
    const data = res?.data || {}
    isWished.value = !!data.wished
    if (activity.value) {
      activity.value.wishCount = data.wishCount != null ? data.wishCount : (activity.value.wishCount || 0)
    }
    ElMessage.success(isWished.value ? '已标记为想看' : '已取消想看')
  } catch {
    // 错误由拦截器统一提示
  } finally {
    wishLoading.value = false
  }
}

const handleReserve = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!canReserve.value) {
    ElMessage.warning('本场未开放预约，请于开售时直接抢票')
    return
  }
  // 拉取票档供用户选择想要的价位
  reservationTicketPrices.value = []
  reservedPriceId.value = null
  try {
    const res = await request.get(`/ticket-price/activity/${activityId.value}`)
    reservationTicketPrices.value = Array.isArray(res.data) ? res.data : []
  } catch {
    reservationTicketPrices.value = []
  }
  showReserveDialog.value = true
}

const selectReservedPrice = (tp) => {
  reservedPriceId.value = tp.id
}

const confirmReserve = async () => {
  actionLoading.value = true
  try {
    const selectedTp = reservationTicketPrices.value.find(tp => tp.id === reservedPriceId.value)
    await applyReservation({
      activityId: activityId.value,
      seatType: selectedTp ? selectedTp.seatType : undefined,
      ticketPriceId: selectedTp ? selectedTp.id : undefined
    })
    ElMessage.success(`预约成功！距离开售${countdownText.value ? '：' + countdownText.value : ''}，开售前将及时提醒您`)
    showReserveDialog.value = false
  } catch {
    // error handled by interceptor
  } finally {
    actionLoading.value = false
  }
}

// ========== 抢票弹窗相关方法 ==========

// 拉取票档列表（免登录接口），未登录也可打开弹窗查看票档
const fetchTicketPrices = async () => {
  ticketPriceLoading.value = true
  try {
    const res = await request.get(`/ticket-price/activity/${activityId.value}`)
    ticketPrices.value = Array.isArray(res.data) ? res.data : []
    // 默认选中第一个有票的票档
    const first = ticketPrices.value.find(tp => getTicketRemain(tp) > 0)
    selectedPriceId.value = first ? first.id : null
  } catch {
    ticketPrices.value = []
    selectedPriceId.value = null
  } finally {
    ticketPriceLoading.value = false
  }
}

// 拉取当前用户常用观演人列表（已登录）
const fetchAttendees = async () => {
  if (!userStore.isLoggedIn) {
    attendees.value = []
    return
  }
  try {
    const res = await getAttendeeList()
    const list = res.data || []
    attendees.value = Array.isArray(list) ? list : []
    // 默认观演人自动带入第一个观演人槽位（未登录时槽位保持为空）
    if (grabAttendeeList.value.length) {
      const def = attendees.value.find(a => a.isDefault === true || a.isDefault === 1)
      if (def && !grabAttendeeList.value[0].attendeeName && !grabAttendeeList.value[0].attendeeIdCard) {
        grabAttendeeList.value[0].attendeeName = def.name || ''
        grabAttendeeList.value[0].attendeeIdCard = def.idCard || ''
        grabAttendeeList.value[0].attendeeId = def.id
      }
    }
  } catch {
    attendees.value = []
  }
}

const openGrabDialog = () => {
  // 重置表单状态（未登录也可打开查看票档）
  grabCount.value = 1
  grabAttendeeList.value = [{ attendeeName: '', attendeeIdCard: '', attendeeId: null }]
  syncGrabAttendeeSlots(grabCount.value)
  selectedSeats.value = []
  showGrabDialog.value = true
  fetchTicketPrices()
  fetchAttendees()
  fetchSeatMap()
}

// 拉取座位图（仅选座场次）
const fetchSeatMap = async () => {
  if (seatMapEnabled.value !== 1) {
    seatMap.value = null
    return
  }
  seatLoading.value = true
  try {
    const res = await getSeatMap(activityId.value)
    seatMap.value = res.data || null
    selectedSeats.value = []
  } catch {
    seatMap.value = null
  } finally {
    seatLoading.value = false
  }
}

// 点击座位：切换选中；达到购票数量上限或座位不可选时给出提示
const toggleSeat = (seat) => {
  if (seat.status === 'sold' || seat.status === 'locked') {
    ElMessage.warning('该座位已不可选')
    return
  }
  const idx = selectedSeats.value.indexOf(seat.code)
  if (idx >= 0) {
    selectedSeats.value.splice(idx, 1)
  } else {
    if (selectedSeats.value.length >= grabCount.value) {
      ElMessage.warning(`最多只能选择 ${grabCount.value} 个座位（与购票数量一致）`)
      return
    }
    selectedSeats.value.push(seat.code)
    lastSelectedSeat.value = seat.code
  }
}

const selectTicketPrice = (tp) => {
  if (getTicketRemain(tp) <= 0) return
  selectedPriceId.value = tp.id
  if (grabCount.value > maxGrabCount.value) {
    grabCount.value = maxGrabCount.value
  }
}

// 从常用观演人选择填充对应槽位
const handleAttendeeSelect = (idx, id) => {
  const slot = grabAttendeeList.value[idx]
  if (!slot) return
  const a = attendees.value.find(x => x.id === id)
  if (a) {
    slot.attendeeName = a.name || ''
    slot.attendeeIdCard = a.idCard || ''
    slot.attendeeId = a.id
  } else {
    // 清空选择时同时清空手动填入内容，避免只留半对信息
    slot.attendeeName = ''
    slot.attendeeIdCard = ''
    slot.attendeeId = null
  }
}

const maskIdCard = (idCard) => {
  const s = String(idCard || '')
  if (s.length < 8) return s
  return s.slice(0, 4) + '**********' + s.slice(-4)
}

const confirmGrab = async () => {
  // 防连点：请求进行中直接拦截重复点击，避免并发重复下单
  if (submittingGrab.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再抢票')
    router.push('/login')
    return
  }
  // 先检查是否已有同活动待支付订单，有则引导去支付，防止重复占座
  if (pendingOrder.value) {
    try {
      await ElMessageBox.confirm(
        '您已有该活动的待支付订单，请先完成支付或取消后再重新抢票。',
        '存在待支付订单',
        {
          confirmButtonText: '去支付',
          cancelButtonText: '知道了',
          type: 'warning',
          distinguishCancelAndClose: true
        }
      )
      goPayPendingOrder()
    } catch {
      // 用户点了"知道了"，不做任何操作
    }
    return
  }
  if (activity.value && activity.value.remainTickets <= 0) {
    ElMessage.warning('本场活动已售罄')
    return
  }
  if (ticketPrices.value.length && !selectedTicketPrice.value) {
    ElMessage.warning('请选择票档')
    return
  }
  if (selectedTicketPrice.value && getTicketRemain(selectedTicketPrice.value) <= 0) {
    ElMessage.warning('所选票档已售罄，请更换票档')
    return
  }
  // 观演人校验（按活动实名模式）：
  // 一票一实名(2)：每张票都必须是实名，观演人数量必须等于购票数量
  // 一单一实名(1)：只需填写 1 位实名购票人，即可购买多张
  // 免实名(0)：可匿名；若填写则每张票需成对补齐
  const mode = realNameMode.value
  const slots = grabAttendeeList.value || []
  const filledAttendees = slots.filter(s => (s.attendeeName || '').trim() && (s.attendeeIdCard || '').trim())
  let halfFilled = false
  for (const s of slots) {
    const hasName = !!(s.attendeeName || '').trim()
    const hasCard = !!(s.attendeeIdCard || '').trim()
    if (hasName !== hasCard) {
      halfFilled = true
      break
    }
  }
  if (halfFilled) {
    ElMessage.warning('请将观演人姓名与身份证号成对填写完整')
    return
  }
  if (mode === 2) {
    if (!filledAttendees.length) {
      ElMessage.warning('本场为“一票一实名”，每张门票都必须填写一位观演人身份证')
      return
    }
    if (filledAttendees.length !== grabCount.value) {
      ElMessage.warning(`共购 ${grabCount.value} 张票，已填写 ${filledAttendees.length} 位观演人，请为每张票补齐观演人（一票一实名）`)
      return
    }
  } else if (mode === 1) {
    if (!filledAttendees.length) {
      ElMessage.warning('本场为“一单一实名”，请填写 1 位实名购票人身份证即可购买多张')
      return
    }
    if (filledAttendees.length > 1) {
      ElMessage.warning('本场为“一单一实名”，只需 1 位实名购票人即可，请勿重复填写')
      return
    }
  } else if (filledAttendees.length && filledAttendees.length !== grabCount.value) {
    ElMessage.warning(`共购 ${grabCount.value} 张票，已填写 ${filledAttendees.length} 位观演人，请为每张票补齐观演人；全部留空则为匿名购买`)
    return
  }
  for (const s of filledAttendees) {
    if ((s.attendeeIdCard || '').trim().length !== 18) {
      ElMessage.warning('请填写 18 位有效身份证号')
      return
    }
  }
  // 选座场次：座位数必须与购票数量一致
  if (seatMapEnabled.value === 1) {
    if (!selectedSeats.value.length) {
      ElMessage.warning('本场为选座场次，请先在座位图中选座')
      return
    }
    if (selectedSeats.value.length !== grabCount.value) {
      ElMessage.warning(`已选 ${selectedSeats.value.length} 个座位，与购票数量 ${grabCount.value} 不一致`)
      return
    }
  }
  submittingGrab.value = true
  actionLoading.value = true
  try {
    await grabTicket({
      activityId: activityId.value,
      quantity: grabCount.value,
      ...(selectedTicketPrice.value ? { ticketPriceId: selectedTicketPrice.value.id } : {}),
      ...(seatMapEnabled.value === 1 ? { seats: selectedSeats.value } : {}),
      attendees: filledAttendees.map(s => ({
        attendeeName: s.attendeeName.trim(),
        attendeeIdCard: s.attendeeIdCard.trim()
      }))
    })
    ElMessage.success('抢票成功！请尽快完成支付')
    showGrabDialog.value = false
    // 抢票成功后刷新待支付订单状态，顶部横幅会自动显示
    await checkPendingOrder()
    // 重新拉取活动详情（库存已减少）
    fetchDetail()
  } catch {
    // error handled by interceptor；失败后刷新票档余票，便于重试
    fetchTicketPrices()
  } finally {
    submittingGrab.value = false
    actionLoading.value = false
  }
}

const handleFavorite = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  try {
    if (isFavorited.value) {
      await removeFavorite(activityId.value)
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(activityId.value)
      isFavorited.value = true
      ElMessage.success('收藏成功')
    }
  } catch {
    // error handled by interceptor
  }
}

// 到货提醒开关（独立于收藏，写入 activity_notify 表）
const handleNotify = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再开启到货提醒')
    router.push('/login')
    return
  }
  notifyLoading.value = true
  try {
    const res = await toggleNotify(activityId.value)
    const data = res?.data || {}
    notifyRegistered.value = !!data.notified
    ElMessage.success(notifyRegistered.value ? '已开启到货提醒，补票/重开将通知你' : '已取消到货提醒')
  } catch {
    // error handled by interceptor
  } finally {
    notifyLoading.value = false
  }
}

// ========== 评价相关方法 ==========

const fetchReviews = async () => {
  reviewLoading.value = true
  try {
    const res = await getPublicReviews(activityId.value)
    const list = res.data?.records || res.data?.list || res.data || []
    reviews.value = Array.isArray(list) ? list : []
  } catch {
    reviews.value = []
  } finally {
    reviewLoading.value = false
  }
}

const fetchReviewStatistics = async () => {
  try {
    const res = await getReviewStatistics(activityId.value)
    reviewStatistics.value = res.data || {}
  } catch {
    reviewStatistics.value = {}
  }
}

const checkPurchaseStatus = async () => {
  if (!userStore.isLoggedIn) {
    hasPurchased.value = false
    return
  }
  try {
    // 查询已完成订单，判断是否购买过该活动
    const res = await getMyOrders({ status: 1, page: 1, size: 1000 })
    const list = res.data?.records || res.data?.list || []
    hasPurchased.value = list.some(item =>
      String(item.activityId) === String(activityId.value)
    )
  } catch {
    hasPurchased.value = false
  }
}

// ========== 待支付订单检查 ==========
const checkPendingOrder = async () => {
  if (!userStore.isLoggedIn) {
    pendingOrder.value = null
    stopPendingOrderTimer()
    return
  }
  try {
    const res = await getMyOrders({ status: 0, page: 1, size: 1000 })
    const list = res.data?.records || res.data?.list || []
    const found = list.find(item =>
      String(item.activityId) === String(activityId.value)
    )
    if (found) {
      pendingOrder.value = found
      startPendingOrderTimer()
    } else {
      pendingOrder.value = null
      stopPendingOrderTimer()
    }
  } catch {
    pendingOrder.value = null
    stopPendingOrderTimer()
  }
}

const updatePendingCountdown = () => {
  if (!pendingOrder.value || !pendingOrder.value.expireTime) {
    pendingOrderCountdown.value = ''
    return
  }
  const now = new Date().getTime()
  const expire = new Date(pendingOrder.value.expireTime).getTime()
  if (isNaN(expire)) {
    pendingOrderCountdown.value = ''
    return
  }
  const diff = expire - now
  if (diff <= 0) {
    pendingOrderCountdown.value = '已过期'
    stopPendingOrderTimer()
    // 已过期，重新检查订单状态
    checkPendingOrder()
    return
  }
  const minutes = Math.floor(diff / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)
  const pad = (n) => String(n).padStart(2, '0')
  pendingOrderCountdown.value = `${pad(minutes)}:${pad(seconds)}`
}

const startPendingOrderTimer = () => {
  stopPendingOrderTimer()
  updatePendingCountdown()
  pendingOrderTimer = setInterval(updatePendingCountdown, 1000)
}

const stopPendingOrderTimer = () => {
  if (pendingOrderTimer) {
    clearInterval(pendingOrderTimer)
    pendingOrderTimer = null
  }
}

const goPayPendingOrder = async () => {
  if (!pendingOrder.value) return
  try {
    await ElMessageBox.confirm(
      `确认支付订单 ¥${Number(pendingOrder.value.totalAmount || 0).toFixed(2)}？`,
      '支付确认',
      { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning' }
    )
    await payOrder(pendingOrder.value.id)
    ElMessage.success('支付成功！')
    pendingOrder.value = null
    stopPendingOrderTimer()
    // 注意：这里不能再调用 fetchDetail()。支付成功后紧接着就跳转订单页，
    // 详情页的刷新请求会在路由切换后才返回，此时 route.params.id 已为空，
    // 会连带发出一串 /xxx/undefined 的请求并弹出“系统内部错误”。页面即将销毁，刷新也无意义。
    router.push('/user/center/orders')
  } catch {
    // 用户取消或支付失败
  }
}

const cancelPendingOrder = async () => {
  if (!pendingOrder.value) return
  try {
    await ElMessageBox.confirm(
      '确定要取消该订单吗？取消后库存将释放给其他用户。',
      '取消订单',
      { confirmButtonText: '确定取消', cancelButtonText: '返回', type: 'warning' }
    )
    await cancelOrder(pendingOrder.value.id)
    ElMessage.success('订单已取消，库存已释放')
    pendingOrder.value = null
    stopPendingOrderTimer()
    fetchDetail()
  } catch {
    // 用户取消
  }
}

const getStarCount = (star) => {
  if (!reviewStatistics.value) return 0
  return Number(reviewStatistics.value[`rating${star}Count`] || 0)
}

const getStarPercentage = (star) => {
  const total = Number(reviewStatistics.value?.totalCount || 0)
  if (total === 0) return 0
  const count = getStarCount(star)
  return Math.round((count / total) * 100)
}

const parseImages = (images) => {
  if (!images) return []
  if (Array.isArray(images)) return images
  try {
    const parsed = JSON.parse(images)
    if (Array.isArray(parsed)) return parsed
  } catch {
    // 不是 JSON，按分隔符处理
    return String(images).split(',').map(s => s.trim()).filter(Boolean)
  }
  return []
}

const openReviewDialog = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (!hasPurchased.value) {
    ElMessage.warning('仅已购票用户可发表评价')
    return
  }
  reviewForm.value = {
    score: 5,
    content: '',
    fileList: []
  }
  reviewDialogVisible.value = true
}

const handleUploadExceed = () => {
  ElMessage.warning('最多上传 6 张图片')
}

const submitReview = async () => {
  if (!reviewForm.value.score || reviewForm.value.score < 1) {
    ElMessage.warning('请选择评分')
    return
  }
  if (!reviewForm.value.content || !reviewForm.value.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  submitReviewLoading.value = true
  try {
    // 取每张图片的真实服务器 URL（上传成功时已在 on-success 中写入 file.url）
    const images = reviewForm.value.fileList
      .map(f => f.url || (typeof f.response?.data === 'string' ? f.response.data : null))
      .filter(Boolean)
    await addReview({
      activityId: activityId.value,
      rating: reviewForm.value.score,
      content: reviewForm.value.content.trim(),
      images: images.length ? JSON.stringify(images) : ''
    })
    ElMessage.success('评价发布成功，审核通过后将展示')
    reviewDialogVisible.value = false
    await fetchReviews()
    await fetchReviewStatistics()
  } catch {
    // error handled by interceptor
  } finally {
    submitReviewLoading.value = false
  }
}

onMounted(() => {
  fetchDetail()
})

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  stopPendingOrderTimer()
})
</script>

<style scoped>
.activity-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 待支付订单提醒横幅 */
.pending-order-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 24px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #fff7f8, #fff0e8);
  border: 1px solid #ffd6cc;
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 16px rgba(255, 77, 109, 0.12);
}

.banner-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.banner-icon {
  font-size: 28px;
  color: #ff4d6d;
  flex-shrink: 0;
  animation: hot-bounce 2s ease-in-out infinite;
}

.banner-content {
  flex: 1;
  min-width: 0;
}

.banner-title {
  font-size: 16px;
  font-weight: 700;
  color: #ff4d6d;
  margin-bottom: 6px;
}

.banner-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banner-amount {
  color: #ff4d6d;
  font-weight: 700;
}

.banner-sep {
  color: #dcdfe6;
  margin: 0 8px;
}

.banner-countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.banner-countdown .el-icon {
  color: #ff7a45;
}

.countdown-digits {
  font-size: 18px;
  font-weight: 800;
  color: #ff4d6d;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
  padding: 2px 8px;
  background: rgba(255, 77, 109, 0.1);
  border-radius: 6px;
}

.countdown-tip {
  font-size: 12px;
  color: #c0c4cc;
}

.banner-right {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.pay-now-btn {
  background: var(--gradient-grab);
  border: none;
  font-weight: 700;
  padding: 0 24px;
  box-shadow: var(--shadow-grab);
  transition: all 0.3s var(--ease-out);
}

.pay-now-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-grab-hover);
  background: var(--gradient-grab-deep);
}

/* 上方信息区 */
.detail-top {
  display: flex;
  gap: 30px;
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 20px;
}

.detail-cover {
  flex-shrink: 0;
  width: 420px;
  position: relative;
}

.cover-image {
  width: 100%;
  height: 340px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.12);
  transition: transform 0.4s var(--ease-out);
}

.cover-image:hover {
  transform: scale(1.02);
}

.image-placeholder {
  width: 420px;
  height: 340px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f2f5, #e8ebf0);
  color: #c0c4cc;
  font-size: 60px;
  border-radius: var(--radius-lg);
}

.detail-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-left: 8px;
}

.detail-title {
  font-size: 26px;
  color: var(--text-primary);
  margin-bottom: 14px;
  line-height: 1.4;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.info-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-regular);
  font-size: 14px;
  margin-bottom: 10px;
}

.info-row .el-icon {
  color: var(--brand-primary);
  font-size: 16px;
}

.info-price {
  margin-top: 14px;
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff7f8, #fff0f5);
  border-radius: var(--radius-md);
  border: 1px solid #ffe0e8;
}

.price-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.price-value {
  font-size: 32px;
  color: #ff4d6d;
  font-weight: 800;
  line-height: 1;
  text-shadow: 0 2px 8px rgba(255, 77, 109, 0.15);
}

.info-remain {
  font-size: 14px;
  color: var(--text-regular);
  margin-top: 6px;
}

.remain-count {
  font-weight: 700;
  color: #409eff;
  font-size: 16px;
}

.info-reserve {
  font-size: 14px;
  color: var(--text-regular);
  margin-top: 6px;
}

.reserve-count {
  font-weight: 700;
  color: #e6a23c;
  font-size: 16px;
}

/* 预约弹窗：预约人数提示与票档选择 */
.reserve-crowd-tip {
  margin-top: 8px;
  padding: 8px 12px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  font-size: 13px;
  color: #b88230;
}

.reserve-crowd-tip b {
  color: #e6a23c;
}

.reserve-seat-section {
  margin-top: 12px;
}

.reserve-seat-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.reserve-seat-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.reserve-seat-card {
  flex: 1;
  min-width: 130px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.reserve-seat-card:hover {
  border-color: #e6a23c;
}

.reserve-seat-card.selected {
  border-color: #e6a23c;
  background: #fdf6ec;
}

.reserve-seat-name {
  font-size: 14px;
  color: var(--text-primary);
}

.reserve-seat-price {
  font-size: 15px;
  font-weight: 700;
  color: #f56c6c;
}

.reserve-seat-note {
  margin-top: 12px;
  font-size: 13px;
  color: #909399;
}

/* 操作区域 */
.action-area {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.grab-btn {
  font-size: 18px;
  padding: 14px 48px;
  letter-spacing: 2px;
  border-radius: 28px;
  background: var(--gradient-grab);
  border: none;
  box-shadow: var(--shadow-grab);
  animation: pulse-grab 1.5s ease-in-out infinite;
  transition: all 0.3s var(--ease-out);
}

.grab-btn:hover:not(:disabled) {
  background: var(--gradient-grab-deep);
  box-shadow: var(--shadow-grab-hover);
  transform: translateY(-2px) scale(1.03);
}

.grab-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
}

.grab-btn.is-disabled,
.grab-btn:disabled {
  background: #dcdfe6;
  box-shadow: none;
  animation: none;
}

.countdown {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: bold;
  background: linear-gradient(135deg, #fff8f0, #fff3f5);
  border: 1px solid #ffe0d6;
  border-radius: 12px;
  padding: 8px 16px;
  box-shadow: 0 2px 10px rgba(255, 122, 69, 0.1);
}

.countdown-label {
  color: #909399;
  font-weight: normal;
  font-size: 13px;
}

.countdown-value {
  color: #ff7a45;
  font-size: 20px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
  font-weight: 800;
}

.countdown.selling {
  background: linear-gradient(135deg, #fff0f3, #ffe8e8);
  border-color: #ffc4d0;
}

.countdown.selling .countdown-value {
  color: #ff4d6d;
  animation: countdown-tick 1s ease-out;
}

/* 已售进度条 */
.sold-progress {
  margin-top: 10px;
  width: 100%;
  max-width: 420px;
}

.sold-progress-text {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}

.sold-progress-text.hot {
  color: #f56c6c;
  font-weight: bold;
}

.hot-flag {
  margin-left: 6px;
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fbc4c4;
  border-radius: 10px;
  padding: 0 8px;
  font-size: 12px;
}

/* ========== 抢票弹窗 ========== */
.grab-dialog .grab-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.grab-activity-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.grab-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.grab-section-title.seat-title {
  flex-wrap: wrap;
}

.seat-3d-btn {
  margin-left: auto;
}

/* 3D 场馆弹窗：系统安排座位的说明 */
.venue3d-assign-tip {
  margin-bottom: 10px;
  padding: 8px 14px;
  font-size: 13px;
  color: #8a6116;
  background: #fdf6ec;
  border: 1px solid #f5dab1;
  border-radius: 6px;
}

.grab-section-title .section-sub {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
}

/* 票档卡片 */
.ticket-price-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ticket-price-card {
  position: relative;
  flex: 1 1 44%;
  min-width: 200px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border: 1.5px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.ticket-price-card:hover {
  border-color: #f56c6c;
}

.ticket-price-card.selected {
  border-color: #f56c6c;
  background: #fef6f6;
  box-shadow: 0 0 0 1px #f56c6c inset;
}

.ticket-price-card.disabled {
  cursor: not-allowed;
  background: #fafafa;
  opacity: 0.65;
}

.ticket-price-card.disabled .tp-price {
  color: #c0c4cc;
}

.tp-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.tp-remain {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.tp-remain.warn {
  color: #f56c6c;
  font-weight: bold;
}

.tp-remain.out {
  color: #c0c4cc;
}

.tp-price {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
}

.tp-check {
  position: absolute;
  top: -1px;
  right: -1px;
  width: 20px;
  height: 20px;
  border-radius: 0 8px 0 8px;
  background: #f56c6c;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.ticket-price-loading {
  padding: 4px 0;
}

/* 购票数量 */
.quantity-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.limit-tip {
  font-size: 12px;
  color: #909399;
}

/* 实名观演人 */
.attendee-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.attendee-select {
  width: 100%;
  margin-bottom: 10px;
}

/* 多观演人动态槽位 */
.attendee-slot {
  border: 1px dashed #e4e7ed;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 10px;
  background: #fafafa;
}

.attendee-slot-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.attendee-slot-label {
  font-size: 13px;
  color: #606266;
  font-weight: 600;
  flex-shrink: 0;
  margin-right: 10px;
}

.attendee-slot-select {
  width: 220px;
  flex-shrink: 0;
}

.attendee-empty {
  font-size: 12px;
  color: #c0c4cc;
}

.attendee-inputs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.attendee-inputs .el-input {
  flex: 1;
}

/* 金额明细 */
.amount-detail {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.amount-line {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #606266;
}

.amount-line.total {
  border-top: 1px dashed #e4e7ed;
  padding-top: 10px;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.amount-total {
  color: #f56c6c;
  font-size: 22px;
  font-weight: 700;
}

/* 协议提示 */
.agreement-tip {
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
}

.agreement-tip.agreement-no {
  color: #f56c6c;
  font-weight: 500;
}

/* 购票须知 */
.notice-list {
  margin: 0;
  padding-left: 18px;
}

.notice-list li {
  margin-bottom: 6px;
}

.notice-no {
  color: #f56c6c;
  font-weight: 600;
}

.notice-yes {
  color: #67c23a;
  font-weight: 600;
}

.notice-extra {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.notice-extra-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.notice-extra-body {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}

.ended-tag {
  font-size: 16px;
  padding: 8px 24px;
}

.favorite-area {
  margin-top: 16px;
}

/* 下方Tab */
.detail-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
}

.tab-content {
  line-height: 1.8;
  color: #606266;
  font-size: 14px;
  min-height: 100px;
}

.tab-content :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

/* ===== 活动介绍 Tab 增强：基本信息卡 + 场馆位置卡 ===== */
.intro-basic {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1px;
  background: #f0f2f5;
  border: 1px solid #f0f2f5;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 18px;
}

.ib-item {
  background: #fafbfc;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.ib-label {
  font-size: 12px;
  color: #909399;
}

.ib-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ib-value.ib-hot {
  color: #ff4d6d;
  font-weight: 600;
  white-space: normal;
}

.intro-desc {
  margin-bottom: 18px;
}

.intro-venue {
  display: flex;
  gap: 16px;
  align-items: center;
  background: linear-gradient(135deg, #f7fbff, #eef4ff);
  border: 1px solid #e3ecff;
  border-radius: 12px;
  padding: 16px 18px;
}

.iv-map {
  position: relative;
  flex-shrink: 0;
  width: 96px;
  height: 72px;
  border-radius: 10px;
  background:
    repeating-linear-gradient(0deg, transparent 0 11px, rgba(43, 108, 184, 0.08) 11px 12px),
    repeating-linear-gradient(90deg, transparent 0 11px, rgba(43, 108, 184, 0.08) 11px 12px),
    #eaf1ff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid #d6e4ff;
  overflow: hidden;
}

.iv-pin {
  font-size: 26px;
  line-height: 1;
  filter: drop-shadow(0 2px 3px rgba(255, 77, 109, 0.4));
}

.iv-map-label {
  position: absolute;
  bottom: 4px;
  font-size: 11px;
  color: #2b6cb8;
  background: rgba(255, 255, 255, 0.85);
  padding: 0 6px;
  border-radius: 4px;
}

.iv-info {
  flex: 1;
  min-width: 0;
}

.iv-name {
  font-size: 16px;
  font-weight: 700;
  color: #1b2a4a;
  margin-bottom: 6px;
}

.iv-tip {
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}

@media (max-width: 768px) {
  .intro-basic {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ========== 评价区域 ========== */
.review-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-top: 20px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

/* 评价统计 */
.review-statistics {
  display: flex;
  gap: 40px;
  padding: 20px;
  background: #fafafa;
  border-radius: 6px;
  margin-bottom: 20px;
}

.stat-left {
  flex: 0 0 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-right: 1px solid #ebeef5;
  padding-right: 40px;
}

.avg-score {
  font-size: 42px;
  font-weight: 700;
  color: #f56c6c;
  line-height: 1;
}

.avg-rate {
  margin: 4px 0;
}

.total-count {
  font-size: 13px;
  color: #909399;
}

.stat-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.star-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.star-label {
  flex: 0 0 36px;
  font-size: 13px;
  color: #606266;
}

.star-progress {
  flex: 1;
}

/* 评价列表 */
.review-list {
  display: flex;
  flex-direction: column;
}

.review-empty {
  padding: 20px 0;
}

.review-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.review-item:last-child {
  border-bottom: none;
}

.review-avatar {
  flex-shrink: 0;
}

.review-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.review-nickname {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.review-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: auto;
}

.review-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.review-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.review-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}

/* 评价图片加载失败的兜底 */
.review-image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  color: #c0c4cc;
  font-size: 22px;
}

/* 首屏骨架屏 */
.detail-skeleton {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 20px;
}

.skeleton-top {
  display: flex;
  gap: 30px;
  margin-bottom: 24px;
}

.skeleton-cover {
  width: 420px;
  flex-shrink: 0;
}

.skeleton-info {
  flex: 1;
  min-width: 0;
}

.skeleton-tabs {
  margin-top: 8px;
}

.review-reply {
  background: #f7f7f7;
  border-radius: 4px;
  padding: 8px 12px;
  margin-top: 4px;
}

.reply-tag {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.reply-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

/* 评价对话框 */
.review-activity-name {
  font-weight: 600;
  color: #303133;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* ========== 在线选座（对标大麦「在线选座」） ========== */
.seat-loading,
.seat-empty {
  padding: 12px 0;
  color: #909399;
  font-size: 13px;
}

.seat-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #606266;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.seat-dot {
  width: 12px;
  height: 12px;
  border-radius: 3px;
  display: inline-block;
}

.seat-dot.available { background: #ffffff; border: 1px solid #dcdfe6; }
.seat-dot.selected { background: #f56c6c; border: 1px solid #f56c6c; }
.seat-dot.sold { background: #c0c4cc; border: 1px solid #c0c4cc; }
.seat-dot.locked { background: #f5dab1; border: 1px solid #f5dab1; }

.seat-section {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  background: #fafbfc;
}

.seat-section-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.seat-section-price {
  margin-left: 8px;
  color: #f56c6c;
  font-weight: 700;
}

.seat-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.seat-row-label {
  width: 36px;
  font-size: 11px;
  color: #909399;
  text-align: right;
  flex-shrink: 0;
}

.seat-row-seats {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.seat-cell {
  width: 24px;
  height: 24px;
  line-height: 22px;
  text-align: center;
  font-size: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  background: #fff;
  color: #606266;
  cursor: pointer;
  padding: 0;
  transition: all 0.15s;
}

.seat-cell:hover:not(:disabled) {
  border-color: #f56c6c;
  color: #f56c6c;
}

.seat-cell.selected {
  background: #f56c6c;
  border-color: #f56c6c;
  color: #fff;
  font-weight: 700;
}

.seat-cell.sold {
  background: #f5f5f5;
  color: #c0c4cc;
  border-color: #ebeef5;
  cursor: not-allowed;
  text-decoration: line-through;
}

.seat-cell.locked {
  background: #fdf3e6;
  color: #e6a23c;
  border-color: #f5dab1;
  cursor: not-allowed;
}

.selected-seats {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
}

.selected-seat-tag {
  display: inline-block;
  margin: 2px 4px;
  padding: 2px 8px;
  background: #fef6f6;
  border: 1px solid #fbc4c4;
  color: #f56c6c;
  border-radius: 10px;
  font-size: 12px;
  font-family: monospace;
}

/* 响应式 */
@media (max-width: 768px) {
  .activity-detail-page {
    max-width: 100%;
    box-sizing: border-box;
    padding: 12px;
  }

  /* 待支付订单横幅：纵向堆叠 */
  .pending-order-banner {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 14px 16px;
  }

  .banner-left {
    gap: 10px;
  }

  .banner-icon {
    font-size: 22px;
  }

  .banner-right {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .banner-right :deep(.el-button) {
    margin-left: 0;
    width: 100%;
  }

  .countdown-digits {
    font-size: 16px;
  }

  .detail-top {
    flex-direction: column;
    gap: 16px;
    padding: 14px;
  }
  .detail-cover {
    width: 100%;
  }
  .cover-image {
    height: 240px;
  }
  .image-placeholder {
    width: 100%;
    height: 240px;
    font-size: 44px;
  }

  .detail-info {
    padding-left: 0;
  }

  .detail-title {
    font-size: 20px;
    margin-bottom: 10px;
  }

  .price-value {
    font-size: 26px;
  }

  /* 抢票等主操作按钮在窄屏撑满 */
  .action-area {
    gap: 10px;
  }

  .grab-btn {
    width: 100%;
    font-size: 16px;
    padding: 12px 24px;
    letter-spacing: 1px;
  }

  .action-area :deep(.el-button) {
    margin-left: 0;
  }

  .favorite-area :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  .sold-progress {
    max-width: 100%;
  }

  /* 评价统计区 */
  .review-statistics {
    flex-direction: column;
    gap: 16px;
  }
  .stat-left {
    flex: none;
    border-right: none;
    border-bottom: 1px solid #ebeef5;
    padding-right: 0;
    padding-bottom: 16px;
    width: 100%;
  }

  .review-top {
    flex-wrap: wrap;
    gap: 6px;
  }

  .review-time {
    font-size: 12px;
  }

  /* 评价图片网格 */
  .review-images {
    gap: 6px;
  }

  .review-image {
    width: 80px;
    height: 80px;
  }

  /* 抢票弹窗内的票档卡片改为单列 */
  .ticket-price-card {
    flex: 1 1 100%;
    min-width: 0;
  }

  .attendee-inputs {
    flex-direction: column;
    gap: 8px;
  }

  .attendee-inputs :deep(.el-input) {
    width: 100%;
  }

  /* 骨架屏在窄屏改为纵向堆叠 */
  .detail-skeleton {
    padding: 14px;
  }

  .skeleton-top {
    flex-direction: column;
    gap: 16px;
  }

  .skeleton-cover {
    width: 100%;
  }
}

/* 观演温馨提示卡片（聚合场馆/入场/取票/儿童/退改等实用信息，让详情页更实用） */
.detail-tips-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px 22px;
  margin-top: 22px;
  box-shadow: 0 3px 14px rgba(31, 92, 170, 0.07);
  border: 1px solid #eef1f6;
}
.tips-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.tips-icon {
  font-size: 18px;
  line-height: 1;
}
.tips-title {
  font-size: 17px;
  font-weight: 700;
  color: #1b2a4a;
  letter-spacing: 0.5px;
}
.tips-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 28px;
}
.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  line-height: 1.6;
}
.tip-item.tip-full {
  grid-column: 1 / -1;
}
.tip-label {
  flex-shrink: 0;
  width: 64px;
  color: #909399;
  font-weight: 600;
}
.tip-value {
  flex: 1;
  min-width: 0;
  color: #303133;
}
.tip-value.tip-warn {
  color: #f56c6c;
  font-weight: 600;
}

/* 相关推荐（同分类在售活动） */
.related-section {
  margin-top: 34px;
}

/* ===== 演出阵容 / 嘉宾 ===== */
.lineup-section {
  margin-top: 34px;
  background: #fff;
  border-radius: 16px;
  padding: 22px 24px 26px;
  border: 1px solid #eef1f6;
  box-shadow: 0 3px 12px rgba(31, 92, 170, 0.06);
}

.lineup-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 18px;
  margin-top: 16px;
}

.lineup-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 18px 12px;
  border-radius: 14px;
  background: linear-gradient(180deg, #fff7f8, #fff);
  border: 1px solid #ffe3e8;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.lineup-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 26px rgba(255, 77, 109, 0.16);
}

.lineup-avatar {
  background: linear-gradient(135deg, #ff7a45, #ff4d6d);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  box-shadow: 0 6px 14px rgba(255, 77, 109, 0.3);
}

.lineup-name {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #1b2a4a;
}

.lineup-role {
  margin-top: 4px;
  font-size: 12px;
  color: #ff4d6d;
  background: rgba(255, 77, 109, 0.1);
  border-radius: 10px;
  padding: 1px 10px;
  display: inline-block;
}

.lineup-desc {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.related-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.28s ease, box-shadow 0.28s ease;
  margin-bottom: 20px;
  border: 1px solid #eef1f6;
  box-shadow: 0 3px 12px rgba(31, 92, 170, 0.06);
}
.related-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 18px 40px rgba(31, 92, 170, 0.16);
  border-color: rgba(255, 77, 109, 0.2);
}
.related-cover-wrap {
  position: relative;
  background: #eef1f6;
  overflow: hidden;
}
.related-cover {
  width: 100%;
  height: 150px;
  display: block;
  transition: transform 0.5s ease;
}
.related-card:hover .related-cover {
  transform: scale(1.07);
}
.related-info {
  padding: 12px 14px 14px;
}
.related-title {
  font-size: 14px;
  font-weight: 600;
  color: #1b2a4a;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}
.related-card:hover .related-title {
  color: #ff4d6d;
}
.related-location {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.related-location .el-icon {
  color: #c0c4cc;
  font-size: 13px;
}
.related-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px dashed #eef1f6;
}
.related-price {
  color: #ff4d6d;
  font-size: 18px;
  font-weight: 800;
  font-style: italic;
}
.related-remain {
  font-size: 12px;
  color: #909399;
}
</style>
