<template>
  <el-card>
  <div style="margin: 10px 0">
    <el-input style="width: 200px" placeholder="请输入名称" suffix-icon="el-icon-search" v-model="name"></el-input>
<!--    <el-input style="width: 200px" placeholder="请输入邮箱" suffix-icon="el-icon-message" class="ml-5" v-model="email"></el-input>-->
<!--    <el-input style="width: 200px" placeholder="请输入地址" suffix-icon="el-icon-position" class="ml-5" v-model="address"></el-input>-->
    <el-button class="ml-5" type="primary" @click="load">搜索</el-button>
    <el-button type="warning" @click="reset">刷新</el-button>
  </div>

  <!-- <div style="margin: 10px 0">
    <el-button type="primary" @click="handleAdd" title="">注册<i class="el-icon-circle-plus-outline"></i></el-button>
  </div> -->

  <el-table :data="tableData"  :header-cell-class-name="headerBg" @selection-change="handleSelectionChange">
    <!-- <el-table-column prop="id" label="ID"></el-table-column> -->
    <el-table-column prop="name" label="设备名称" ></el-table-column>
    <el-table-column prop="device_id" label="设备ID" width="110">
      <template slot-scope="scope">
        <span v-if="scope.row.device_id">{{ scope.row.device_id }}</span>
        <span v-else style="color: #999;">—</span>
      </template>
    </el-table-column>
    <el-table-column prop="ip" label="IP" ></el-table-column>
    <el-table-column prop="pin" label="PIN码"></el-table-column>
    <el-table-column prop="username" label="所属用户" ></el-table-column>
    <el-table-column prop="status" label="状态" >
      <template slot-scope="scope">
      {{ scope.row.status === 'Online' ? '在线' : '离线' }}
    </template>
    </el-table-column>
    <el-table-column prop="createdTime" label="创建时间" ></el-table-column>
    <el-table-column label="操作" align="center">
      <template slot-scope="scope">
        <div class="button-container">
        <el-popconfirm
              class="ml-5"
              confirm-button-text='确定'
              cancel-button-text='我再想想'
              icon="el-icon-info"
              icon-color="red"
              title="您确定删除吗？删除后不可恢复，请谨慎操作！"
              @confirm="del(scope.row.id)"
          >
            <el-button round type="danger" slot="reference">删除</el-button>
          </el-popconfirm>
          <el-button round type="success" @click="handleEdit(scope.row)">编辑</el-button>
        </div>
      </template>
    </el-table-column>
    <el-table-column label=""  align="center">
      <template slot-scope="scope">
        <div class="button-container">
          <el-button round type="success" @click="distribute(scope.row)">分配</el-button>
          <el-button round type="danger" @click="recycle(scope.row)">回收</el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
  <!--        分页组件-->
  <div style="padding: 10px 0">
    <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pageNum"
        :page-sizes="[5, 10, 15, 20]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total">
    </el-pagination>
  </div>



  <el-dialog title="设备信息" :visible.sync="dialogFormVisible" width="50%" >
    <el-form label-width="80px" size="small">
      <el-form-item label="IP">
        <el-input v-model="addForm.ip" autocomplete="off"></el-input>
      </el-form-item>

    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogFormVisible = false">取 消</el-button>
      <el-button type="primary" @click="save">确 定</el-button>
    </div>
  </el-dialog>

  <el-dialog title="设备信息" :visible.sync="dialogUpdateFormVisible" width="30%" >
    <el-form label-width="80px" size="small">
      <el-form-item label="IP">
        <el-input v-model="editForm.ip" autocomplete="off"></el-input>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogUpdateFormVisible = false">取 消</el-button>
      <el-button type="primary" @click="update(editForm.id)">修改</el-button>
    </div>
  </el-dialog>


  <el-dialog title="用户信息" :visible.sync="dialogDistributeVisible" width="30%" >
    <el-form label-width="80px" size="small">
      <el-form-item label="用户">
        <el-select v-model="distributeForm.userId" placeholder="请选择用户" style="width: 100%">
          <el-option v-for="item in users" :key="item.name" :label="item.name" :value="item.id">
            {{ item.name }}
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="dialogDistributeVisible = false">取 消</el-button>
      <el-button type="primary" @click="distributeSave()">绑定</el-button>
    </div>
  </el-dialog>

  </el-card>
</template>

<script>

export default {
  name: "Device",
  data(){
    return{
      tableData:[],
      total:0,
      pageNum:1,
      pageSize:5,
      name:'',
      username: '',
      virtualIp: '',
      status: '',
      createdTime: '',
      gpuForm: {
        gpuId: '',
        deviceName: ''
      },
      addForm: {
        ip:''
      },
      distributeForm: {
        userId: ''
      },
      editForm: {},
      configForm: {
        memoryInfo: '',
        cpuInfo: ''
      },
      dialogFormVisible: false,
      dialogUpdateFormVisible: false,
      dialogConfigFormVisible: false,
      dialogDistributeVisible: false,
      menuDialogVis:false,
      menuData:[],
      props:{
        label: 'name',
      },
      expends:[],
      checks:[],
      roleId:1,
      multipleSelection: [],
      headerBg:"headerBg",
      userInfos: [],
      timer: null,
      users: [],
    }
  },
  created() {
    this.load();
  },
  mounted() {
    this.timer = setInterval(this.load, 10000);
  },
  beforeDestroy() {
    clearInterval(this.timer);
  },
  methods:{
    load() {
      this.request.get("/api/stream/v1/devices?count="+this.pageSize+"&index="+this.pageNum+"&name="+this.name).then(res => {
        this.tableData = res.data.data.devices
        this.total = res.data.data.totalNum
      })
    },
    save() {
      this.request.post("/api/stream/v1/device", this.addForm).then(res => {
        if (res.status===200) {
          if (res.data.msg == "Device.Exist") {
            this.$message.error("设备已存在")
          } else if (res.data.msg == "Common.InvalidParam") {
            this.$message.error("参数异常，请检查参数后再重试！")
          } else {
            this.$message.success("设备注册成功")
          }
          this.dialogFormVisible = false
          this.load()
        } else {
          this.$message.error("设备注册失败")
        }
      })
    },

    update(id) {
      this.request.put("/api/stream/v1/device/" + id, this.editForm).then(res => {
        if (res.status===200) {
          if (res.data.msg == "Common.InvalidParam") {
            this.$message.error("参数异常，请检查参数后再重试！")
          } else {
            this.$message.success("更新成功")
          }
          this.dialogUpdateFormVisible = false
          this.load()
        } else {
          this.$message.error("保存失败")
        }
      })
    },

    handleAdd() {
      this.dialogFormVisible = true
      this.addForm = {}
    },

    handleEdit(row) {
      this.editForm = row
      this.dialogUpdateFormVisible = true
    },
    distribute(row) {
      this.distributeForm.deviceId = row.id
      this.dialogDistributeVisible = true
      this.request.get("/api/stream/v1/users_all").then(res => {
        this.users = res.data.data.users
      })
    },
    recycle(row) {
      console.log(row)
      let unbind_form = {
        deviceId: row.id
      }
      this.request.post("/api/stream/v1/recycle", unbind_form).then(res => {
        if (res.status === 200) {
          if (res.data.msg == "Common.InvalidParam") {
            this.$message.error("参数异常，请检查参数后再重试！")
          } else if (res.data.msg == "Device.NotBind") {
            this.$message.error("设备未分配，请检查后重试！")
          } else {
            this.$message.success("回收成功")
            this.distributeForm = {}
            this.dialogDistributeVisible = false
          }
          this.load()
        } else {
          this.$message.error("回收失败")
        }
      })
    },
    distributeSave() {
      this.request.post("/api/stream/v1/distribute", this.distributeForm).then(res => {
        if (res.status === 200) {
          if (res.data.msg == "Common.InvalidParam") {
            this.$message.error("参数异常，请检查参数后再重试！")
          } else if (res.data.msg == "Device.AlreadyBind") {
            this.$message.error("设备已绑定用户，请回收后再进行绑定!")
          } else {
            this.$message.success("分配成功")
            this.distributeForm = {}
            this.dialogDistributeVisible = false
          }
          this.load()
        } else {
          this.$message.error("分配失败")
        }
      })
    },
    // handleFileChange(field, file) {
    //   console.log(field)
    //   console.log(file)
    // },
    // handleSuccess(file) {
    //   this.addForm.srcVmPath = file.raw.name
    //   console.log(file)
    //   console.log(URL.createObjectURL(file.raw))
    //   console.log(document.getElementsByClassName("el-upload__input")[0].value)
    // },
    handleVMConfig(row) {
      // console.log(row)
      this.configForm = row
      this.dialogConfigFormVisible = true
    },
    setVMConfig() {
      console.log(this.configForm)
    },
    openVM(id) {
      let obj = {
        "vm_id": id,
        "action": 1
      }
      this.request.post("/api/stream/v1/vm/operate", obj).then(res => {
        if (res.status===200) {
          if (res.data.code == 2) {
            this.$message.error("无法操作，请检查设备的状态或联系管理员！")
          } else {
            this.$message.success("操作成功")
          }
          this.load()
        } else {
          this.$message.error("操作失败")
        }
      })
    },
    closeVM(id) {
      let obj = {
        "vm_id": id,
        "action": 2
      }
      this.request.post("/api/stream/v1/vm/operate", obj).then(res => {
        if (res.status===200) {
          if (res.data.code == 2) {
            this.$message.error("无法操作，请检查设备的状态或联系管理员！")
          } else {
            this.$message.success("操作成功")
          }
          this.load()
        } else {
          this.$message.error("操作失败")
        }
      })
    },
    resetVM(id) {
      let obj = {
        "vm_id": id,
        "action": 3
      }
      this.request.post("/api/stream/v1/vm/operate", obj).then(res => {
        if (res.status===200) {
          if (res.data.code == 2) {
            this.$message.error("无法操作，请检查设备的状态或联系管理员！")
          } else {
            this.$message.success("操作成功")
          }
          this.load()
        } else {
          this.$message.error("操作失败")
        }
      })
    },

    del(id) {
      this.request.delete("/api/stream/v1/device/" + id).then(res => {
        if (res.status===200) {
          if (res.data.msg == "Device.IsRunning") {
            this.$message.error("设备处于运行状态，无法删除，请先关闭设备后再操作！")
          } else {
            this.$message.success("删除成功")
          }
          this.load()
        } else {
          this.$message.error("删除失败")
        }
      })
    },

    handleSelectionChange(val) {
      console.log(val)
      this.multipleSelection = val
    },

    reset() {
      this.name = ""
      this.load()
    },

    handleSizeChange(pageSize) {
      console.log(pageSize)
      this.pageSize = pageSize
      this.load()
    },
    handleCurrentChange(pageNum) {
      console.log(pageNum)
      this.pageNum = pageNum
      this.load()
    }

  }
}
</script>

<style>
.headerBg {
  background: #eee !important;
}

.button-container {
  display: flex;
  flex-direction: column;
  align-items: center; /* 将按钮容器内元素垂直对齐方式设置为顶部对齐 */
}

.button-container > * {
  margin-bottom: 5px; /* 调整按钮之间的垂直间距 */
}

.button-container .el-button {
  width: 100px; /* 设置按钮宽度 */
  height: 30px; /* 设置按钮高度 */
}


</style>