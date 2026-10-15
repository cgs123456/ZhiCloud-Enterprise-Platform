<!-- TMS GPS 定位（接收型域：report 为唯一写入口，其余为只读查询；无 CRUD） -->
<template>
  <ContentWrap>
    <el-tabs v-model="activeTab">
      <!-- 位置上报 -->
      <el-tab-pane label="位置上报" name="report">
        <el-form
          ref="reportFormRef"
          :model="reportForm"
          :rules="reportRules"
          label-width="110px"
          class="max-w-560px"
        >
          <el-form-item label="车辆" prop="vehicleId">
            <el-input-number
              v-model="reportForm.vehicleId"
              :controls="false"
              class="!w-1/1"
              placeholder="请输入车辆编号"
            />
          </el-form-item>
          <el-form-item label="运单编号" prop="shipmentId">
            <el-input-number
              v-model="reportForm.shipmentId"
              :controls="false"
              class="!w-1/1"
              placeholder="请输入运单编号（可空）"
            />
          </el-form-item>
          <el-form-item label="经度" prop="longitude">
            <el-input-number
              v-model="reportForm.longitude"
              :controls="false"
              :precision="6"
              class="!w-1/1"
              placeholder="请输入经度，如 116.4074"
            />
          </el-form-item>
          <el-form-item label="纬度" prop="latitude">
            <el-input-number
              v-model="reportForm.latitude"
              :controls="false"
              :precision="6"
              class="!w-1/1"
              placeholder="请输入纬度，如 39.9042"
            />
          </el-form-item>
          <el-form-item label="速度" prop="speed">
            <el-input-number v-model="reportForm.speed" :controls="false" :precision="2" class="!w-1/1" placeholder="km/h（可空）" />
          </el-form-item>
          <el-form-item label="方向" prop="direction">
            <el-input-number v-model="reportForm.direction" :controls="false" :min="0" :max="360" class="!w-1/1" placeholder="0-360 度（可空）" />
          </el-form-item>
          <el-form-item label="上报时间" prop="reportTime">
            <el-date-picker
              v-model="reportForm.reportTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="留空则由服务端记录当前时间"
              class="!w-1/1"
            />
          </el-form-item>
          <el-form-item label="位置描述" prop="locationDesc">
            <el-input v-model="reportForm.locationDesc" maxlength="255" placeholder="请输入位置描述" />
          </el-form-item>
          <el-form-item>
            <el-button :loading="reportLoading" type="primary" @click="submitReport">
              <Icon icon="ep:position" class="mr-5px" /> 上报位置
            </el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 车辆最新位置 -->
      <el-tab-pane label="车辆最新位置" name="latest">
        <div class="flex gap-8px mb-12px">
          <el-input-number v-model="latestVehicleId" :controls="false" class="!w-240px" placeholder="车辆编号" />
          <el-button type="primary" :loading="latestLoading" @click="loadLatest">
            <Icon icon="ep:search" class="mr-5px" /> 查询
          </el-button>
        </div>
        <el-empty v-if="!latestLoading && !latestPosition" description="暂无该车辆的位置记录" />
        <el-descriptions v-if="latestPosition" :column="3" border>
          <el-descriptions-item label="车辆编号">{{ latestPosition.vehicleId }}</el-descriptions-item>
          <el-descriptions-item label="运单编号">{{ latestPosition.shipmentId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ latestPosition.reportTime ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="经度">{{ latestPosition.longitude }}</el-descriptions-item>
          <el-descriptions-item label="纬度">{{ latestPosition.latitude }}</el-descriptions-item>
          <el-descriptions-item label="速度(km/h)">{{ latestPosition.speed ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="方向">{{ latestPosition.direction ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="位置描述">{{ latestPosition.locationDesc ?? '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- 轨迹查询 -->
      <el-tab-pane label="轨迹查询" name="track">
        <div class="flex flex-col gap-8px mb-12px">
          <div class="flex gap-8px">
            <el-input-number
              v-model="trackForm.shipmentId"
              :controls="false"
              class="!w-240px"
              placeholder="运单编号（按运单查轨迹）"
            />
            <el-button type="primary" :loading="trackLoading" @click="loadTrack">
              <Icon icon="ep:search" class="mr-5px" /> 按运单查轨迹
            </el-button>
          </div>
          <el-divider content-position="left">或按车辆 + 时间区间查询</el-divider>
          <div class="flex flex-wrap gap-8px">
            <el-input-number
              v-model="trackForm.vehicleId"
              :controls="false"
              class="!w-240px"
              placeholder="车辆编号"
            />
            <el-date-picker
              v-model="trackForm.timeRange"
              type="datetimerange"
              range-separator="~"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DDTHH:mm:ss"
              class="!w-420px"
            />
            <el-button type="primary" :loading="trackLoading" @click="loadVehicleTrack">
              <Icon icon="ep:search" class="mr-5px" /> 查车辆轨迹
            </el-button>
          </div>
        </div>
        <el-empty v-if="!trackLoading && tracks.length === 0" description="暂无轨迹数据" />
        <template v-if="tracks.length > 0">
          <!-- 轨迹图：优先百度地图（真地图）；未配置 AK / 加载失败时回退 echarts 折线+散点 -->
          <div
            v-if="mapMode === 'baidu'"
            ref="mapContainerRef"
            class="w-full h-[460px] border-rounded"
          ></div>
          <Echart v-else :height="420" :options="trackChartOption" />
          <el-table :data="tracks" size="small" stripe>
            <el-table-column align="center" label="上时间" prop="reportTime" width="200" />
            <el-table-column align="center" label="车辆编号" prop="vehicleId" width="100" />
            <el-table-column align="center" label="运单编号" prop="shipmentId" width="100" />
            <el-table-column align="right" label="经度" prop="longitude" width="140" />
            <el-table-column align="right" label="纬度" prop="latitude" width="140" />
            <el-table-column align="right" label="速度" prop="speed" width="100" />
            <el-table-column align="left" label="位置描述" prop="locationDesc" min-width="180" show-overflow-tooltip />
          </el-table>
        </template>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { TmsGpsApi } from '@/api/tms/gps'
import type { TmsGpsPositionVO } from '@/api/tms/gps'
import type { EChartsOption } from 'echarts'
import { loadBaiduMapSdk } from '@/components/Map/src/utils'

defineOptions({ name: 'TmsGps' })

const message = useMessage()

const activeTab = ref('report')

/** 位置上报 */
const reportLoading = ref(false)
const reportFormRef = ref()
const reportForm = ref<any>({
  vehicleId: undefined,
  shipmentId: undefined,
  longitude: undefined,
  latitude: undefined,
  speed: undefined,
  direction: undefined,
  reportTime: undefined,
  locationDesc: undefined
})
const reportRules = reactive({
  vehicleId: [{ required: true, message: '车辆编号不能为空', trigger: 'blur' }],
  longitude: [{ required: true, message: '经度不能为空', trigger: 'blur' }],
  latitude: [{ required: true, message: '纬度不能为空', trigger: 'blur' }]
})

const submitReport = async () => {
  await reportFormRef.value.validate()
  reportLoading.value = true
  try {
    const data: TmsGpsPositionVO = {
      vehicleId: reportForm.value.vehicleId,
      shipmentId: reportForm.value.shipmentId || undefined,
      longitude: reportForm.value.longitude,
      latitude: reportForm.value.latitude,
      speed: reportForm.value.speed,
      direction: reportForm.value.direction,
      reportTime: reportForm.value.reportTime || undefined,
      locationDesc: reportForm.value.locationDesc
    }
    await TmsGpsApi.reportTmsGpsPosition(data)
    message.success('位置已上报')
  } finally {
    reportLoading.value = false
  }
}

/** 车辆最新位置 */
const latestLoading = ref(false)
const latestVehicleId = ref<number>()
const latestPosition = ref<TmsGpsPositionVO | null>(null)

const loadLatest = async () => {
  if (!latestVehicleId.value) {
    message.warning('请输入车辆编号')
    return
  }
  latestLoading.value = true
  try {
    latestPosition.value = (await TmsGpsApi.getLatestTmsGpsPosition(latestVehicleId.value)) || null
  } finally {
    latestLoading.value = false
  }
}

/** 轨迹查询 */
const trackLoading = ref(false)
const trackForm = ref<any>({ shipmentId: undefined, vehicleId: undefined, timeRange: undefined })
const tracks = ref<TmsGpsPositionVO[]>([])

const loadTrack = async () => {
  if (!trackForm.value.shipmentId) {
    message.warning('请输入运单编号')
    return
  }
  trackLoading.value = true
  try {
    tracks.value = (await TmsGpsApi.getShipmentTrack(trackForm.value.shipmentId)) || []
  } finally {
    trackLoading.value = false
  }
}

const loadVehicleTrack = async () => {
  if (!trackForm.value.vehicleId || !trackForm.value.timeRange?.length) {
    message.warning('请输入车辆编号并选择时间区间')
    return
  }
  trackLoading.value = true
  try {
    tracks.value =
      (await TmsGpsApi.getVehicleTrack({
        vehicleId: trackForm.value.vehicleId,
        startTime: trackForm.value.timeRange[0],
        endTime: trackForm.value.timeRange[1]
      })) || []
  } finally {
    trackLoading.value = false
  }
}

const mapMode = ref<'baidu' | 'chart'>('chart') // 真地图 / echarts 兜底
const mapContainerRef = ref<HTMLElement>()
let mapInstance: any = null

/**
 * 尝试初始化百度地图轨迹；AK 未配置或 SDK 加载失败时回退 echarts。
 * 项目已内置百度地图（components/Map，IoT 设备地图等 4 处在用），但 .env 未配
 * VITE_BAIDU_MAP_KEY 时 SDK 会 reject——必须兜底，否则把能用的图改成不可用。
 */
const initBaiduMap = async (points: any[]) => {
  try {
    await loadBaiduMapSdk()
    if (!mapContainerRef.value || !window.BMapGL) {
      mapMode.value = 'chart'
      return
    }
    if (mapInstance) {
      mapInstance.destroy?.()
      mapInstance = null
    }
    const bmap = new window.BMapGL.Map(mapContainerRef.value)
    bmap.enableScrollWheelZoom()
    bmap.addControl(new window.BMapGL.ScaleControl())
    bmap.addControl(new window.BMapGL.ZoomControl())
    // 轨迹折线 + 起终点/途经点标注
    const bPoints = points.map((p) => new window.BMapGL.Point(p.value[0], p.value[1]))
    if (bPoints.length > 1) {
      bmap.addOverlay(
        new window.BMapGL.Polyline(bPoints, { strokeColor: '#409EFF', strokeWeight: 4, strokeOpacity: 0.8 })
      )
    }
    points.forEach((p, idx) => {
      const isStart = idx === 0
      const isEnd = idx === points.length - 1 && points.length > 1
      const color = isStart || isEnd ? '#f56c6c' : '#409Eff'
      const mk = new window.BMapGL.Marker(bPoints[idx], {
        icon: new window.BMapGL.Symbol(window.BMapGL.Symbol_TYPE.BACKWARD_CLOSED_ARROW, {
          scale: isStart || isEnd ? 1.4 : 1,
          fillColor: color,
          fillOpacity: 0.9,
          strokeColor: '#fff',
          strokeWeight: 1
        })
      })
      const label = isStart ? '起点' : isEnd ? '终点' : `G${idx}`
      const content = `<div style="padding:6px;font-size:12px;line-height:1.7">
        <b>${label}</b><br/>${p.time ?? '-'}<br/>速度 ${p.speed ?? '-'} km/h<br/>${p.desc ?? ''}</div>`
      mk.addEventListener('click', () => {
        bmap.openInfoWindow(new window.BMapGL.InfoWindow(content, { width: 200 }), bPoints[idx])
      })
      bmap.addOverlay(mk)
    })
    bmap.setViewport(bPoints)
    mapInstance = bmap
    mapMode.value = 'baidu'
  } catch {
    // AK 未配置 / SDK 失败 / 无网：静默回退 echarts（不阻塞查询功能）
    mapMode.value = 'chart'
  }
}

/** 查询后重绘：有坐标数据时尝试地图，否则直接兜底 */
watch(tracks, async (val) => {
  if (!val || val.length === 0) {
    mapMode.value = 'chart'
    return
  }
  const pts = val.map((t: any) => ({
    value: [Number(t.longitude), Number(t.latitude)],
    time: t.reportTime,
    speed: t.speed,
    desc: t.locationDesc
  }))
  await nextTick()
  await initBaiduMap(pts)
})

/** 轨迹图：echarts 兜底（无地图时）——经纬度直绘折线 + 上报点散点 */
const trackChartOption = computed<EChartsOption>(() => {
  const points = tracks.value.map((t: any) => ({
    value: [Number(t.longitude), Number(t.latitude)],
    time: t.reportTime,
    speed: t.speed,
    desc: t.locationDesc
  }))
  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => {
        const d = points[p.dataIndex]
        if (!d) return ''
        return `经度 ${d.value[0]}<br/>纬度 ${d.value[1]}<br/>上报时间 ${d.time ?? '-'}<br/>速度 ${d.speed ?? '-'} km/h<br/>位置 ${d.desc ?? '-'}`
      }
    },
    grid: { left: 60, right: 40, top: 30, bottom: 40 },
    xAxis: { type: 'value', scale: true, name: '经度', nameLocation: 'middle', nameGap: 25 },
    yAxis: { type: 'value', scale: true, name: '纬度', nameLocation: 'middle', nameGap: 40 },
    series: [
      {
        name: '行驶轨迹',
        type: 'line',
        data: points,
        showSymbol: false,
        lineStyle: { width: 2 },
        smooth: false
      },
      {
        name: '上报点',
        type: 'scatter',
        data: points,
        symbolSize: 8,
        itemStyle: { color: '#f56c6c' }
      }
    ]
  }
})
</script>
