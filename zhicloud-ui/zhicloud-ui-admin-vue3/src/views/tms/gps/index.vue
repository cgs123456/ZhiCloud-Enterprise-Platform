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
        <el-table v-if="tracks.length > 0" :data="tracks" size="small" stripe>
          <el-table-column align="center" label="上时间" prop="reportTime" width="200" />
          <el-table-column align="center" label="车辆编号" prop="vehicleId" width="100" />
          <el-table-column align="center" label="运单编号" prop="shipmentId" width="100" />
          <el-table-column align="right" label="经度" prop="longitude" width="140" />
          <el-table-column align="right" label="纬度" prop="latitude" width="140" />
          <el-table-column align="right" label="速度" prop="speed" width="100" />
          <el-table-column align="left" label="位置描述" prop="locationDesc" min-width="180" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { TmsGpsApi } from '@/api/tms/gps'
import type { TmsGpsPositionVO } from '@/api/tms/gps'

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
</script>
