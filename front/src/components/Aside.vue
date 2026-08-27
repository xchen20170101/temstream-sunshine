<template>
  <el-menu
      :default-openeds="['1', '3']" style="min-height: 100vh;overflow-x: hidden;border-right-color: transparent"
      background-color="rgb(48,65,86)"
      text-color="#fff"
      active-text-color="#ffd04b"
      :collapse="isCollapse"
      :collapse-transition="false"
      router
      default-active="/home"
  >
    <div style="height: 60px;text-align: center;line-height: 60px">
      <img src="../assets/logo.png" width="30px" style="position: relative;top: 7px">
      <b style="color:#ffffff;" v-show="!isCollapse">设备管理平台</b>
    </div>

<!--    <div v-for="item in menus" :key="item.id">-->
<!--      <div v-if="item.path">-->
<!--        <el-menu-item :index="item.path">-->
<!--          <i :class="item.icon"></i>-->
<!--          <span slot="title">{{ item.name }}</span>-->
<!--        </el-menu-item>-->
<!--      </div>-->
<!--      <div v-else>-->
<!--        <el-submenu :index="item.id + ''">-->
<!--          <template slot="title">-->
<!--            <i :class="item.icon"></i>-->
<!--            <span slot="title" v-if="!isCollapse">{{ item.name }}</span>-->
<!--          </template>-->
<!--          <div  v-for="subItem in item.children" :key="subItem.id">-->
<!--            <el-menu-item :index="subItem.path">-->
<!--              <i :class="subItem.icon"></i>-->
<!--              <span slot="title">{{ subItem.name }}</span>-->
<!--            </el-menu-item>-->
<!--          </div>-->
<!--        </el-submenu>-->
<!--      </div>-->
<!--    </div>-->
    <el-menu-item index="/home">
        <i class="el-icon-s-home"></i>
        <span slot="title">主页</span>
    </el-menu-item>
    <el-menu-item index="/user">
        <i class="el-icon-s-custom"></i>
        <span slot="title">用户管理</span>
    </el-menu-item>
    <el-menu-item index="/device">
        <i class="el-icon-s-platform"></i>
        <span slot="title">设备管理</span>
    </el-menu-item>
  </el-menu>
</template>

<script>
export default {
  name: "Aside",
  props: {
    isCollapse: Boolean,
    menus:Array
  }
}
</script>

<style scoped>

</style>