<template>
  <div class="course-detail page-full">
    <div class="page-header">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div v-if="course" class="header-info">
        <h2>{{ course.name }}</h2>
        <p>{{ course.description }}</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="course-tabs">
      <el-tab-pane label="课程概览" name="overview">
        <div class="overview-content">
          <el-card shadow="never">
            <template #header>
              <span>课程信息</span>
            </template>
            <div class="info-grid">
              <div class="info-item">
                <span class="label">课程名称</span>
                <span class="value">{{ course?.name }}</span>
              </div>
              <div class="info-item">
                <span class="label">授课教师</span>
                <span class="value">{{ course?.teacherName }}</span>
              </div>
              <div class="info-item">
                <span class="label">学生人数</span>
                <span class="value">{{ course?.studentCount }} 人</span>
              </div>
              <div class="info-item">
                <span class="label">关联班级</span>
                <span class="value">{{ course?.classNames?.join('、') || '暂无' }}</span>
              </div>
            </div>
          </el-card>

          <el-card v-if="authStore.isTeacher" shadow="never" class="quick-actions">
            <template #header>
              <span>快捷操作</span>
            </template>
            <div class="action-grid">
              <div class="action-item" @click="activeTab = 'attendance'">
                <el-icon :size="32"><Check /></el-icon>
                <span>发起签到</span>
              </div>
              <div class="action-item" @click="activeTab = 'questions'">
                <el-icon :size="32"><QuestionFilled /></el-icon>
                <span>发起提问</span>
              </div>
              <div class="action-item" @click="activeTab = 'votes'">
                <el-icon :size="32"><Star /></el-icon>
                <span>发起投票</span>
              </div>
              <div class="action-item" @click="activeTab = 'homework'">
                <el-icon :size="32"><Document /></el-icon>
                <span>发布作业</span>
              </div>
              <div class="action-item" @click="activeTab = 'files'">
                <el-icon :size="32"><Files /></el-icon>
                <span>上传资料</span>
              </div>

              <div class="action-item" @click="showLotteryDialog = true">
                <el-icon :size="32"><Star /></el-icon>
                <span>随机点名</span>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="签到" name="attendance">
        <div class="tab-content">
          <!-- 教师端：签到活动列表 -->
          <template v-if="authStore.isTeacher">
            <div class="toolbar">
              <el-button type="primary" @click="showSignInDialog = true">
                <el-icon><Plus /></el-icon>
                发起签到
              </el-button>
            </div>
            <div v-loading="loadingActivities" class="activity-list">
              <el-card v-for="activity in activities" :key="activity.id" shadow="never" class="activity-card" @click="viewActivityDetails(activity)">
                <div class="activity-header">
                  <h3>签到活动 #{{ activity.id }}</h3>
                  <el-tag :type="activity.status === 1 ? 'success' : 'info'">
                    {{ activity.status === 1 ? '进行中' : '已结束' }}
                  </el-tag>
                </div>
                <div class="activity-stats">
                  <div class="stat-item">
                    <span class="stat-value">{{ activity.totalStudents || 0 }}</span>
                    <span class="stat-label">总人数</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ activity.signedCount || 0 }}</span>
                    <span class="stat-label">已签到</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-value">{{ activity.unsignedCount || 0 }}</span>
                    <span class="stat-label">未签到</span>
                  </div>
                </div>
                <div class="activity-time">
                  发起时间：{{ new Date(activity.createTime).toLocaleString() }}
                  <span v-if="activity.duration">，持续{{ activity.duration }}分钟</span>
                </div>
              </el-card>
              <el-empty v-if="!loadingActivities && activities.length === 0" description="暂无签到活动" />
            </div>
          </template>
          <!-- 学生端：显示待签到列表 -->
          <template v-else>
            <div v-loading="loadingStudentActivities" class="activity-list">
              <el-card v-for="activity in studentActivities" :key="activity.id" shadow="never" class="activity-card student-activity">
                <div class="activity-header">
                  <h3>{{ activity.courseName }}</h3>
                  <el-tag type="warning">待签到</el-tag>
                </div>
                <div class="activity-time">
                  发起时间：{{ new Date(activity.createTime).toLocaleString() }}，持续{{ activity.duration }}分钟
                </div>
                <el-button type="primary" class="signin-btn" @click="handleStudentSignIn(activity)">
                  立即签到
                </el-button>
              </el-card>
              <el-empty v-if="!loadingStudentActivities && studentActivities.length === 0" description="暂无待签到活动" />
            </div>
          </template>
        </div>
      </el-tab-pane>

      <el-tab-pane label="提问" name="questions">
        <div class="tab-content">
          <div v-if="authStore.isTeacher" class="toolbar">
            <el-button type="primary" @click="showQuestionDialog = true">
              <el-icon><Plus /></el-icon>
              发起提问
            </el-button>
          </div>
          <div v-loading="loadingQuestions" class="question-list">
            <el-card v-for="q in questions" :key="q.id" shadow="never" class="question-card">
              <div class="question-header">
                <el-tag :type="q.type === 1 ? '' : q.type === 2 ? 'warning' : 'info'" size="small">
                  {{ q.type === 1 ? '单选' : q.type === 2 ? '多选' : q.type === 3 ? '填空' : '简答' }}
                </el-tag>
                <el-tag :type="q.status === 1 ? 'success' : 'info'" size="small" style="margin-left: 8px">
                  {{ q.status === 1 ? '进行中' : '已结束' }}
                </el-tag>
                <span class="question-time">{{ new Date(q.createTime).toLocaleString() }}</span>
                <el-button
                  v-if="authStore.isTeacher && q.status === 1"
                  type="danger"
                  link
                  size="small"
                  style="margin-left: auto"
                  @click="handleCloseQuestion(q)"
                >结束提问</el-button>
                <el-button
                  v-if="authStore.isTeacher"
                  type="primary"
                  link
                  size="small"
                  style="margin-left: 8px"
                  @click="openAnswerReview(q)"
                >查看回答</el-button>
              </div>
              <p class="question-content">{{ q.content }}</p>
              <div v-if="q.options && q.options.length > 0" class="question-options">
                <div v-for="opt in q.options" :key="opt.label" class="question-option-line">
                  <span class="question-option-label">{{ opt.label }}.</span>
                  <span>{{ opt.content }}</span>
                </div>
              </div>
              <div v-if="!authStore.isTeacher" class="student-answer-box">
                <template v-if="q.myAnswer">
                  <el-tag type="success">已回答</el-tag>
                  <span class="student-answer-text">我的答案：{{ q.myAnswer }}</span>
                </template>
                <template v-else-if="q.status === 1">
                  <el-radio-group v-if="q.type === 1" v-model="q.selectedAnswer" class="student-answer-input">
                    <el-radio v-for="opt in q.options || []" :key="opt.label" :value="opt.label">
                      {{ opt.label }}. {{ opt.content }}
                    </el-radio>
                  </el-radio-group>
                  <el-checkbox-group v-else-if="q.type === 2" v-model="q.selectedAnswers" class="student-answer-input">
                    <el-checkbox v-for="opt in q.options || []" :key="opt.label" :value="opt.label">
                      {{ opt.label }}. {{ opt.content }}
                    </el-checkbox>
                  </el-checkbox-group>
                  <el-input v-else-if="q.type === 3" v-model="q.fillAnswer" placeholder="请输入答案" class="student-answer-input" />
                  <el-input v-else v-model="q.fillAnswer" type="textarea" :rows="3" placeholder="请输入答案" class="student-answer-input" />
                  <el-button type="primary" size="small" @click="submitStudentAnswer(q)">提交答案</el-button>
                </template>
                <template v-else>
                  <el-tag type="info">未作答</el-tag>
                </template>
              </div>
              <div class="question-stats">
                <span>答题人数：{{ q.answerCount || 0 }}</span>
                <!-- 简答题需要阅卷，不展示“正确人数” -->
                <span v-if="q.type !== 4">正确人数：{{ q.correctCount || 0 }}</span>
                <span>分值：{{ q.points }} 分</span>
              </div>
            </el-card>
            <el-empty v-if="!loadingQuestions && questions.length === 0" description="暂无提问记录" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="投票" name="votes">
        <div class="tab-content">
          <div v-if="authStore.isTeacher" class="toolbar">
            <el-button type="primary" @click="showVoteDialog = true">
              <el-icon><Plus /></el-icon>
              发起投票
            </el-button>
          </div>
          <div v-loading="loadingVotes" class="vote-list">
            <el-card v-for="vote in votes" :key="vote.id" shadow="never" class="vote-card">
              <div class="vote-header">
                <h3>{{ vote.title }}</h3>
                <div>
                  <el-tag :type="vote.status === 1 ? 'success' : 'info'">{{ vote.status === 1 ? '进行中' : '已结束' }}</el-tag>
                  <el-tag size="small" type="warning" style="margin-left: 8px">{{ vote.type === 2 ? '多选' : '单选' }}</el-tag>
                  <el-tag size="small" type="info" style="margin-left: 8px">{{ vote.anonymous === 1 ? '匿名投票' : '实名投票' }}</el-tag>
                  <el-button
                    v-if="authStore.isTeacher && vote.status === 1"
                    link
                    type="danger"
                    style="margin-left: 8px"
                    @click="handleCloseVote(vote.id)"
                  >结束投票</el-button>
                </div>
              </div>
              <div class="vote-options">
                <div v-for="opt in vote.options" :key="opt.key" class="vote-option-line">
                  <div class="vote-option-label">{{ opt.key }}. {{ opt.content }}</div>
                  <div class="vote-option-stat">
                    <el-progress :percentage="opt.percentage || 0" :stroke-width="10" />
                    <span>{{ opt.voteCount || 0 }} 票</span>
                  </div>
                  <div v-if="authStore.isTeacher && vote.anonymous === 0 && opt.voterNames?.length" class="vote-voter-names">
                    投票人：{{ opt.voterNames.join('、') }}
                  </div>
                </div>
              </div>
              <div class="vote-footer">
                <span>总票数：{{ vote.totalVotes || 0 }}</span>
                <span>发起时间：{{ new Date(vote.createTime).toLocaleString() }}</span>
              </div>

              <div v-if="!authStore.isTeacher && vote.status === 1" class="vote-student-box">
                <template v-if="vote.myOptions?.length">
                  <el-tag type="success">已投票</el-tag>
                  <span class="student-answer-text">我的选择：{{ vote.myOptions.join('、') }}</span>
                </template>
                <template v-else>
                  <el-radio-group v-if="vote.type !== 2" v-model="voteSelectionsSingle[vote.id]">
                    <el-radio v-for="opt in vote.options" :key="opt.key" :value="opt.key">{{ opt.key }}. {{ opt.content }}</el-radio>
                  </el-radio-group>
                  <el-checkbox-group v-else v-model="voteSelectionsMulti[vote.id]">
                    <el-checkbox v-for="opt in vote.options" :key="opt.key" :value="opt.key">{{ opt.key }}. {{ opt.content }}</el-checkbox>
                  </el-checkbox-group>
                  <el-button type="primary" size="small" @click="handleSubmitVote(vote.id)">提交投票</el-button>
                </template>
              </div>
            </el-card>
            <el-empty v-if="!loadingVotes && votes.length === 0" description="暂无投票" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="作业" name="homework">
        <div class="tab-content">
          <div v-if="authStore.isTeacher" class="toolbar">
            <el-button type="primary" @click="showHomeworkDialog = true">
              <el-icon><Plus /></el-icon>
              发布作业
            </el-button>
          </div>
          <div v-loading="loadingHomeworks" class="homework-list">
            <el-card
              v-for="hw in homeworks"
              :key="hw.id"
              shadow="never"
              class="homework-card"
              :class="{ 'homework-card-clickable': !authStore.isTeacher }"
              @click="!authStore.isTeacher && goToHomeworkPage(hw)"
            >
              <div class="homework-header">
                <h3>{{ hw.title }}</h3>
                <el-tag v-if="hw.deadline" :type="new Date(hw.deadline) > new Date() ? 'success' : 'danger'">
                  {{ new Date(hw.deadline) > new Date() ? '进行中' : '已截止' }}
                </el-tag>
              </div>
              <p class="homework-content">{{ hw.content || '暂无内容' }}</p>
              <div class="homework-meta">
                <span v-if="hw.chapter">章节：{{ hw.chapter }}</span>
                <span>总分：{{ hw.totalPoints }}分</span>
                <span v-if="hw.deadline">截止：{{ new Date(hw.deadline).toLocaleString() }}</span>
              </div>
            </el-card>
            <el-empty v-if="!loadingHomeworks && homeworks.length === 0" description="暂无作业" />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="资料" name="files">
        <div class="tab-content" v-loading="loadingFiles">
          <div class="toolbar">
            <el-upload
              v-if="authStore.isTeacher"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :data="{ courseId: courseId, type: 3, category: 'materials' }"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>
                上传资料
              </el-button>
            </el-upload>
          </div>
          <div v-if="files.length > 0" class="course-file-list">
            <el-card v-for="f in files" :key="f.id" shadow="never" class="course-file-item">
              <div class="course-file-main" @click="handlePreviewFile(f)">
                <div class="course-file-name">{{ f.fileName }}</div>
                <div class="course-file-meta">
                  <span>{{ formatFileSize(f.fileSize) }}</span>
                  <span>上传时间：{{ new Date(f.createTime).toLocaleString() }}</span>
                </div>
              </div>
              <div class="course-file-actions">
                <el-button link type="primary" @click="handlePreviewFile(f)">预览</el-button>
                <el-button link type="primary" @click="handleDownloadFile(f)">下载</el-button>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无资料" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="学生" name="students" v-if="authStore.isTeacher">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" :disabled="selectedStudentIds.length === 0" @click="showAddPointsDialog = true">
              给选中学生加分 ({{ selectedStudentIds.length }})
            </el-button>
          </div>
          <el-table :data="studentsWithPoints" v-loading="loadingStudents" @selection-change="handleStudentSelectionChange">
            <el-table-column type="selection" width="50" />
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="studentNo" label="学号" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="coursePoints" label="本课程积分" width="120">
              <template #default="{ row }">
                <el-tag type="warning">{{ row.coursePoints || 0 }} 分</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的成绩" name="myScore" v-if="!authStore.isTeacher">
        <div class="tab-content" v-loading="loadingMyScore">
          <el-row :gutter="12">
            <el-col :xs="24" :sm="8">
              <el-card shadow="never">
                <template #header>我的总积分</template>
                <div style="font-size: 28px; font-weight: 600">{{ myCoursePoints }}</div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-card shadow="never">
                <template #header>签到统计</template>
                <div class="score-stat">
                  <div>总次数：{{ myAttendance.total }}</div>
                  <div>已签到：{{ myAttendance.signed }}</div>
                  <div>缺勤：{{ myAttendance.absent }}</div>
                  <div>出勤率：{{ myAttendance.rate }}%</div>
                </div>
              </el-card>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-card shadow="never">
                <template #header>我的排名</template>
                <div style="font-size: 20px; font-weight: 600">{{ myRankText }}</div>
              </el-card>
            </el-col>
          </el-row>

          <el-card shadow="never" style="margin-top: 12px">
            <template #header>积分明细（本课程）</template>
            <el-table :data="myPointsRecords" size="small">
              <el-table-column prop="createTime" label="时间" width="180" />
              <el-table-column prop="points" label="分值" width="90" />
              <el-table-column prop="description" label="说明" />
            </el-table>
            <el-empty v-if="myPointsRecords.length === 0" description="暂无积分明细" />
          </el-card>

          <el-card shadow="never" style="margin-top: 12px">
            <template #header>排行榜（本课程）</template>
            <el-table :data="rankingList" size="small">
              <el-table-column type="index" label="#" width="60" />
              <el-table-column prop="realName" label="姓名" />
              <el-table-column prop="points" label="积分" width="90" />
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 随机点名对话框（教师） -->
    <el-dialog v-model="showLotteryDialog" title="随机点名" width="420px">
      <el-form label-position="top">
        <el-form-item label="抽取人数">
          <el-input-number v-model="lotteryCount" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="加分分值（每人）">
          <el-input-number v-model="lotteryPoints" :min="0" :max="100" />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleLotteryDraw" :loading="lotteryDrawing">开始抽取</el-button>
        </el-form-item>
        <el-form-item v-if="lotterySelected.length > 0" label="抽取结果">
          <div class="lottery-selected">
            <el-tag v-for="u in lotterySelected" :key="u.id" style="margin: 0 6px 6px 0">
              {{ u.realName || u.username }}
            </el-tag>
          </div>
        </el-form-item>
        <el-alert
          v-if="lotterySelected.length > 0"
          type="info"
          :closable="false"
          :title="`确认后将为以上学生各加 ${lotteryPoints} 分`"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="showLotteryDialog = false">取消</el-button>
        <el-button type="primary" :disabled="lotterySelected.length === 0" :loading="lotteryConfirming" @click="handleLotteryConfirm">
          确认加分
        </el-button>
      </template>
    </el-dialog>

    <!-- 签到对话框 -->
    <el-dialog v-model="showSignInDialog" title="发起签到" width="400px">
      <el-form :model="signInForm" label-position="top">
        <el-form-item label="选择班级（不选则向全部班级发起）">
          <el-select v-model="signInForm.classIds" multiple placeholder="选择班级" style="width: 100%">
            <el-option v-for="cls in courseClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="签到时长（分钟）">
          <el-input-number v-model="signInForm.duration" :min="1" :max="60" />
        </el-form-item>
        <el-form-item label="签到位置（可选）">
          <el-input v-model="signInForm.location" placeholder="如：教室A101" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSignInDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAttendance">发起签到</el-button>
      </template>
    </el-dialog>

    <!-- 提问对话框 -->
    <el-dialog v-model="showQuestionDialog" title="发起提问" width="600px">
      <el-form :model="questionForm" label-position="top">
        <el-form-item label="选择班级（不选则向全部班级发起）">
          <el-select v-model="questionForm.classIds" multiple placeholder="选择班级" style="width: 100%">
            <el-option v-for="cls in courseClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题内容">
          <el-input v-model="questionForm.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="问题类型">
          <el-radio-group v-model="questionForm.type">
            <el-radio :value="1">单选题</el-radio>
            <el-radio :value="2">多选题</el-radio>
            <el-radio :value="3">问答题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="questionForm.type !== 3" label="选项">
          <div v-for="(opt, idx) in questionForm.options" :key="idx" class="option-row">
            <el-input v-model="opt.label" placeholder="选项标签" style="width: 80px" />
            <el-input v-model="opt.content" placeholder="选项内容" />
            <el-button type="danger" link @click="questionForm.options.splice(idx, 1)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button text type="primary" @click="questionForm.options.push({ label: '', content: '' })">
            + 添加选项
          </el-button>
        </el-form-item>
        <el-form-item v-if="questionForm.type !== 3" label="正确答案">
          <el-input v-model="questionForm.correctAnswer" placeholder="如：A" />
        </el-form-item>
        <el-form-item label="答题时限（秒）">
          <el-input-number v-model="questionForm.duration" :min="10" :max="600" />
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="questionForm.points" :min="1" :max="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showQuestionDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateQuestion">发起提问</el-button>
      </template>
    </el-dialog>

    <!-- 作业对话框 -->
    <el-dialog v-model="showHomeworkDialog" title="发布作业" width="600px">
      <el-form :model="homeworkForm" label-position="top">
        <el-form-item label="选择班级（不选则向全部班级发布）">
          <el-select v-model="homeworkForm.classIds" multiple placeholder="选择班级" style="width: 100%">
            <el-option v-for="cls in courseClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作业标题">
          <el-input v-model="homeworkForm.title" />
        </el-form-item>
        <el-form-item label="作业内容">
          <el-input v-model="homeworkForm.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="章节">
          <el-input v-model="homeworkForm.chapter" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="homeworkForm.deadline"
            type="datetime"
            placeholder="选择截止时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="总分">
          <el-input-number v-model="homeworkForm.totalPoints" :min="1" :max="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHomeworkDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateHomework">发布作业</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showVoteDialog" title="发起投票" width="600px">
      <el-form :model="voteForm" label-position="top">
        <el-form-item label="选择班级（不选则向全部班级发起）">
          <el-select v-model="voteForm.classIds" multiple placeholder="选择班级" style="width: 100%">
            <el-option v-for="cls in courseClasses" :key="cls.id" :label="cls.name" :value="cls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="投票主题">
          <el-input v-model="voteForm.title" placeholder="例如：下节课复习方式" />
        </el-form-item>
        <el-form-item label="投票模式">
          <el-radio-group v-model="voteForm.type">
            <el-radio :value="1">单选</el-radio>
            <el-radio :value="2">多选</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="匿名设置">
          <el-switch v-model="voteForm.anonymous" active-text="匿名投票" inactive-text="实名投票" />
        </el-form-item>
        <el-form-item label="投票选项">
          <div v-for="(opt, idx) in voteForm.options" :key="idx" class="option-row">
            <el-input v-model="opt.content" placeholder="请输入选项内容" />
            <el-button v-if="voteForm.options.length > 2" type="danger" link @click="voteForm.options.splice(idx, 1)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button text type="primary" @click="voteForm.options.push({ content: '' })">+ 添加选项</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showVoteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateVote">发起投票</el-button>
      </template>
    </el-dialog>

    <!-- 手动加分对话框 -->
    <el-dialog v-model="showAddPointsDialog" title="手动加分" width="400px">
      <el-form :model="addPointsForm" label-position="top">
        <el-form-item label="积分">
          <el-input-number v-model="addPointsForm.points" :min="1" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="addPointsForm.description" placeholder="如：课堂表现优秀" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddPointsDialog = false">取消</el-button>
        <el-button type="primary" :loading="addingPoints" @click="handleAddPoints">确认加分</el-button>
      </template>
    </el-dialog>

    <!-- 签到活动详情对话框 -->
    <el-dialog v-model="showActivityDetailDialog" title="签到详情" width="700px">
      <div v-if="activityDetail" class="activity-detail">
        <el-tabs v-model="activeDetailTab">
          <el-tab-pane label="已签到" name="signed">
            <el-table :data="activityDetail.signedStudents" max-height="300">
              <el-table-column prop="realName" label="姓名" />
              <el-table-column prop="userName" label="用户名" />
              <el-table-column prop="signTime" label="签到时间">
                <template #default="{ row }">
                  {{ row.signTime ? new Date(row.signTime).toLocaleString() : '-' }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="未签到" name="unsigned">
            <el-table :data="activityDetail.unsignedStudents" max-height="300">
              <el-table-column prop="realName" label="姓名" />
              <el-table-column prop="userName" label="用户名" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <!-- 回答查看/阅卷对话框（教师端） -->
    <el-dialog v-model="showAnswerReviewDialog" title="回答列表" width="760px">
      <div v-if="currentReviewQuestion" style="margin-bottom: 12px">
        <div style="font-weight: 600">题目：{{ currentReviewQuestion.content }}</div>
        <div style="margin-top: 6px; color: #666">
          类型：{{ currentReviewQuestion.type === 1 ? '单选' : currentReviewQuestion.type === 2 ? '多选' : currentReviewQuestion.type === 3 ? '填空' : '简答' }}
          ，分值：{{ currentReviewQuestion.points }}
        </div>
        <el-alert
          v-if="currentReviewQuestion.type === 4"
          type="info"
          :closable="false"
          title="简答题需要教师阅卷打分（仅输入得分即可）"
          show-icon
          style="margin-top: 10px"
        />
      </div>

      <el-table :data="reviewAnswers" v-loading="loadingReviewAnswers" max-height="420">
        <el-table-column prop="userName" label="用户名" width="140" />
        <el-table-column prop="content" label="回答内容" />
        <el-table-column prop="score" label="得分" width="90" />
        <el-table-column prop="isCorrect" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.isCorrect === 1" type="success">已判</el-tag>
            <el-tag v-else type="info">未判</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button
              v-if="currentReviewQuestion?.type === 4"
              type="primary"
              link
              @click="openGradeDialog(row)"
            >打分</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showAnswerReviewDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 简答题打分对话框 -->
    <el-dialog v-model="showGradeDialog" title="简答题打分" width="420px">
      <el-form label-position="top">
        <el-form-item label="AI评分建议">
          <div style="display: flex; gap: 8px; align-items: center; margin-bottom: 6px">
            <el-button size="small" type="primary" plain :loading="loadingAiSuggestion" @click="loadAiSuggestion">获取AI建议</el-button>
            <el-button
              size="small"
              type="success"
              plain
              :disabled="!aiSuggestion"
              @click="applyAiSuggestion"
            >填入得分</el-button>
          </div>
          <el-checkbox v-model="autoFetchAiSuggestion" style="margin-bottom: 6px">打开对话框时自动获取</el-checkbox>
          <el-alert
            v-if="aiSuggestion"
            type="info"
            :closable="false"
            show-icon
            :title="`建议得分：${aiSuggestion.suggestedScore}（信心：${aiSuggestion.confidence}）`"
          >
            <template #default>
              <div style="margin-top: 6px">短评：{{ aiSuggestion.feedback || '无' }}</div>
              <div style="margin-top: 4px">要点/扣分：{{ aiSuggestion.criteriaSummary || '无' }}</div>
            </template>
          </el-alert>
          <el-text v-else type="info">点击获取AI建议，供教师参考。</el-text>
        </el-form-item>
        <el-form-item label="得分">
          <el-input-number v-model="gradeScore" :min="0" :max="currentReviewQuestion?.points || 100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGradeDialog = false">取消</el-button>
        <el-button type="primary" :loading="grading" @click="submitGrade">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Check, QuestionFilled, Document, Files, Plus, Upload, Delete, Star } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useCourseStore } from '../stores/course'
import { getCourseStudents } from '../api/course'
import { createAttendance, getCourseActivities, getActivityDetails, getStudentActivities, signIn as studentSignIn } from '../api/attendance'
import { createQuestion, getCourseQuestions, closeQuestion, type Question } from '../api/question'
import { getCoursePointsRanking, addPointsForUsers, type PointsRankingRecord } from '../api/points'
import { createHomework, getCourseHomeworks, type Homework } from '../api/homework'
import { getQuestionAnswers, reviewAnswer, submitAnswer as submitAnswerApi, suggestAnswerGrade, type AiGradeSuggestion } from '../api/answer'
import { drawLottery } from '../api/lottery'
import { getCourseMyPointsRecords, getCourseMyPointsTotal } from '../api/score'
import { getAttendanceStatistics } from '../api/attendance'
import { getCourseFiles, previewFile, downloadFile } from '../api/file'
import { createVote, getCourseVotes, submitVote, closeVote, type Vote } from '../api/vote'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const courseStore = useCourseStore()

const courseId = Number(route.params.id)
const course = computed(() => courseStore.currentCourse)

// 班级列表
const courseClasses = computed(() => {
  if (!course.value) return []
  return course.value.classIds.map((id, i) => ({ id, name: course.value!.classNames[i] || '' }))
})

const activeTab = ref('overview')
const students = ref<any[]>([])
const loadingStudents = ref(false)

// ===================== 学生端：我的成绩 =====================
const loadingMyScore = ref(false)
const myCoursePoints = ref(0)
const myPointsRecords = ref<any[]>([])
const myAttendance = reactive({ total: 0, signed: 0, absent: 0, rate: 0 })
const rankingList = ref<PointsRankingRecord[]>([])
const myRankText = computed(() => {
  const meId = authStore.user?.id
  if (!meId || rankingList.value.length === 0) return '暂无排名'
  const idx = rankingList.value.findIndex(r => r.userId === meId)
  return idx >= 0 ? `第 ${idx + 1} 名 / ${rankingList.value.length}` : '暂无排名'
})

async function loadMyScore() {
  if (authStore.isTeacher) return
  loadingMyScore.value = true
  try {
    const [total, records, stats, ranking] = await Promise.all([
      getCourseMyPointsTotal(courseId),
      getCourseMyPointsRecords(courseId),
      getAttendanceStatistics(courseId),
      getCoursePointsRanking(courseId)
    ])

    myCoursePoints.value = total
    myPointsRecords.value = records

    // 后端现接口返回 List<Attendance>（学生在 course 下的历史签到记录），前端做聚合
    const totalCount = Array.isArray(stats) ? stats.length : 0
    const signedCount = Array.isArray(stats) ? stats.filter((a: any) => a.status === 1).length : 0
    const absentCount = Math.max(0, totalCount - signedCount)
    myAttendance.total = totalCount
    myAttendance.signed = signedCount
    myAttendance.absent = absentCount
    myAttendance.rate = totalCount === 0 ? 0 : Math.round((signedCount * 10000) / totalCount) / 100

    rankingList.value = ranking
  } finally {
    loadingMyScore.value = false
  }
}

watch(activeTab, (val) => {
  if (val === 'myScore') {
    loadMyScore()
  }
})

// 提问列表
const questions = ref<Question[]>([])
const loadingQuestions = ref(false)

// 投票列表
const votes = ref<Vote[]>([])
const loadingVotes = ref(false)
const voteSelectionsSingle = reactive<Record<number, string>>({})
const voteSelectionsMulti = reactive<Record<number, string[]>>({})

// 积分排行
const pointsRanking = ref<PointsRankingRecord[]>([])

// ===================== 教师端：随机点名 =====================
const showLotteryDialog = ref(false)
const lotteryCount = ref(1)
const lotteryPoints = ref(1)
const lotteryDrawing = ref(false)
const lotteryConfirming = ref(false)
const lotterySelected = ref<any[]>([])

async function handleLotteryDraw() {
  lotteryDrawing.value = true
  try {
    lotterySelected.value = await drawLottery(courseId, lotteryCount.value)
  } finally {
    lotteryDrawing.value = false
  }
}

async function handleLotteryConfirm() {
  if (lotterySelected.value.length === 0) return
  lotteryConfirming.value = true
  try {
    await addPointsForUsers({
      courseId,
      userIds: lotterySelected.value.map(u => u.id),
      points: lotteryPoints.value,
      description: `随机点名加分（${lotterySelected.value.length}人）`
    })
    ElMessage.success('加分成功')
    showLotteryDialog.value = false
    lotterySelected.value = []
  } finally {
    lotteryConfirming.value = false
  }
}

// 学生加分
const selectedStudentIds = ref<number[]>([])
const showAddPointsDialog = ref(false)
const addingPoints = ref(false)
const addPointsForm = reactive({
  points: 5,
  description: ''
})

// 学生列表+积分合并展示
const studentsWithPoints = computed(() => {
  return students.value.map(s => {
    const ranking = pointsRanking.value.find(r => r.userId === s.id)
    return { ...s, coursePoints: ranking?.points ?? 0 }
  })
})

// 作业列表
const homeworks = ref<Homework[]>([])
const loadingHomeworks = ref(false)

// 课程资料
const files = ref<any[]>([])
const loadingFiles = ref(false)

// 签到
const showSignInDialog = ref(false)
const signInForm = reactive({
  duration: 15,
  location: '',
  classIds: [] as number[]
})

// 签到活动列表（教师端）
const activities = ref<any[]>([])
const loadingActivities = ref(false)

// 学生待签到活动
const studentActivities = ref<any[]>([])
const loadingStudentActivities = ref(false)

// 签到活动详情
const showActivityDetailDialog = ref(false)
const activityDetail = ref<any>(null)
const activeDetailTab = ref('signed')

// 提问
const showQuestionDialog = ref(false)
const questionForm = reactive({
  content: '',
  type: 1,
  options: [
    { label: 'A', content: '' },
    { label: 'B', content: '' },
    { label: 'C', content: '' },
    { label: 'D', content: '' }
  ],
  correctAnswer: '',
  duration: 60,
  points: 5,
  classIds: [] as number[]
})

// 作业
const showHomeworkDialog = ref(false)
const homeworkForm = reactive({
  title: '',
  content: '',
  chapter: '',
  deadline: '',
  totalPoints: 100,
  classIds: [] as number[]
})

// 投票
const showVoteDialog = ref(false)
const voteForm = reactive({
  title: '',
  classIds: [] as number[],
  type: 1,
  anonymous: true,
  options: [{ content: '' }, { content: '' }]
})

const uploadUrl = '/api/v1/files/upload'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${authStore.token}`
}))

onMounted(async () => {
  await courseStore.fetchCourseById(courseId)
  if (authStore.isTeacher) {
    loadStudents()
    loadPointsRanking()
    loadActivities()
    loadQuestions()
    loadVotes()
  } else {
    loadStudentActivities()
    loadQuestions()
    loadVotes()
  }
  loadHomeworks()
  loadFiles()
})

const loadStudents = async () => {
  loadingStudents.value = true
  try {
    students.value = await getCourseStudents(courseId)
  } finally {
    loadingStudents.value = false
  }
}

const loadPointsRanking = async () => {
  try {
    pointsRanking.value = await getCoursePointsRanking(courseId)
  } catch (e) {
    // ignore
  }
}

const loadQuestions = async () => {
  loadingQuestions.value = true
  try {
    questions.value = ((await getCourseQuestions(courseId)) || []).map((q: any) => normalizeQuestion(q))
  } catch (e) {
    // ignore
  } finally {
    loadingQuestions.value = false
  }
}

const normalizeQuestion = (raw: any) => {
  const q = { ...raw }
  if (q.options && typeof q.options === 'string') {
    try {
      q.options = JSON.parse(q.options)
    } catch {
      q.options = []
    }
  }
  if (!Array.isArray(q.options)) {
    q.options = []
  }
  q.selectedAnswer = ''
  q.selectedAnswers = []
  q.fillAnswer = ''
  if (q.myAnswer) {
    if (q.type === 1) {
      q.selectedAnswer = q.myAnswer
    } else if (q.type === 2) {
      q.selectedAnswers = String(q.myAnswer)
        .replace(/，/g, ',')
        .split(/[\s,]+/)
        .filter(Boolean)
    } else {
      q.fillAnswer = q.myAnswer
    }
  }
  return q
}

const submitStudentAnswer = async (q: any) => {
  const content = q.type === 1
    ? (q.selectedAnswer || '')
    : q.type === 2
      ? (Array.isArray(q.selectedAnswers) ? q.selectedAnswers.join(',') : '')
      : (q.fillAnswer || '')
  if (!content) {
    ElMessage.warning('请先填写答案')
    return
  }
  try {
    await submitAnswerApi(q.id, content)
    q.myAnswer = content
    q.answerCount = (q.answerCount || 0) + 1
    ElMessage.success('回答提交成功')
  } catch (e: any) {
    ElMessage.error(e.message || '回答提交失败')
  }
}

// ===================== 教师端：回答查看/简答阅卷 =====================
const showAnswerReviewDialog = ref(false)
const currentReviewQuestion = ref<Question | null>(null)
const reviewAnswers = ref<any[]>([])
const loadingReviewAnswers = ref(false)

const showGradeDialog = ref(false)
const grading = ref(false)
const gradeScore = ref(0)
const currentGradingAnswerId = ref<number | null>(null)
const aiSuggestion = ref<AiGradeSuggestion | null>(null)
const loadingAiSuggestion = ref(false)
const autoFetchAiSuggestion = ref(true)

const openAnswerReview = async (q: Question) => {
  currentReviewQuestion.value = q
  showAnswerReviewDialog.value = true
  await loadReviewAnswers()
}

const loadReviewAnswers = async () => {
  if (!currentReviewQuestion.value) return
  loadingReviewAnswers.value = true
  try {
    reviewAnswers.value = await getQuestionAnswers(currentReviewQuestion.value.id)
  } catch (e: any) {
    ElMessage.error(e.message || '获取回答失败')
  } finally {
    loadingReviewAnswers.value = false
  }
}

const openGradeDialog = (row: any) => {
  currentGradingAnswerId.value = row.id
  gradeScore.value = Number(row.score) || 0
  aiSuggestion.value = null
  loadingAiSuggestion.value = false
  showGradeDialog.value = true
  if (autoFetchAiSuggestion.value) {
    loadAiSuggestion()
  }
}

const loadAiSuggestion = async () => {
  if (currentGradingAnswerId.value == null) return
  loadingAiSuggestion.value = true
  try {
    aiSuggestion.value = await suggestAnswerGrade(currentGradingAnswerId.value)
  } catch (e: any) {
    ElMessage.error(e.message || '获取AI建议失败')
  } finally {
    loadingAiSuggestion.value = false
  }
}

const applyAiSuggestion = () => {
  if (!aiSuggestion.value) return
  gradeScore.value = Number(aiSuggestion.value.suggestedScore) || 0
}

const submitGrade = async () => {
  if (!currentReviewQuestion.value || currentGradingAnswerId.value == null) return
  grading.value = true
  try {
    await reviewAnswer({
      answerId: currentGradingAnswerId.value,
      // 方案1：简答题以分数为主，不强制对错。这里用“score>0 判为 correct” 仅用于状态展示。
      correct: gradeScore.value > 0,
      score: gradeScore.value
    })
    ElMessage.success('打分成功')
    showGradeDialog.value = false
    await loadReviewAnswers()
    // 打分会产生积分，刷新排行榜/学生列表积分
    await loadPointsRanking()
  } catch (e: any) {
    ElMessage.error(e.message || '打分失败')
  } finally {
    grading.value = false
  }
}

const handleStudentSelectionChange = (rows: any[]) => {
  selectedStudentIds.value = rows.map(r => r.id)
}

const handleAddPoints = async () => {
  if (selectedStudentIds.value.length === 0) return
  if (!addPointsForm.description) {
    ElMessage.warning('请填写加分说明')
    return
  }
  addingPoints.value = true
  try {
    await addPointsForUsers({
      userIds: selectedStudentIds.value,
      courseId,
      points: addPointsForm.points,
      description: addPointsForm.description
    })
    ElMessage.success('加分成功')
    showAddPointsDialog.value = false
    addPointsForm.points = 5
    addPointsForm.description = ''
    selectedStudentIds.value = []
    loadPointsRanking()
  } catch (e: any) {
    ElMessage.error(e.message || '加分失败')
  } finally {
    addingPoints.value = false
  }
}

const handleCloseQuestion = async (q: Question) => {
  try {
    await closeQuestion(q.id)
    ElMessage.success('提问已结束')
    loadQuestions()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const loadHomeworks = async () => {
  loadingHomeworks.value = true
  try {
    homeworks.value = (await getCourseHomeworks(courseId)).records
  } catch (e) {
    // ignore
  } finally {
    loadingHomeworks.value = false
  }
}

const goToHomeworkPage = (hw: any) => {
  router.push({
    path: '/homework',
    query: {
      courseId: String(courseId),
      homeworkId: String(hw.id)
    }
  })
}

// 加载签到活动列表（教师端）
const loadActivities = async () => {
  loadingActivities.value = true
  try {
    activities.value = await getCourseActivities(courseId)
  } catch (e) {
    // ignore
  } finally {
    loadingActivities.value = false
  }
}

// 加载学生待签到活动
const loadStudentActivities = async () => {
  loadingStudentActivities.value = true
  try {
    studentActivities.value = await getStudentActivities()
  } catch (e) {
    // ignore
  } finally {
    loadingStudentActivities.value = false
  }
}

// 查看签到活动详情
const viewActivityDetails = async (activity: any) => {
  try {
    activityDetail.value = await getActivityDetails(activity.id)
    showActivityDetailDialog.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

// 学生签到
const handleStudentSignIn = async (activity: any) => {
  try {
    await studentSignIn({
      courseId: activity.courseId,
      activityId: activity.id
    })
    ElMessage.success('签到成功！')
    loadStudentActivities()
  } catch (e: any) {
    ElMessage.error(e.message || '签到失败')
  }
}

const handleCreateAttendance = async () => {
  try {
    await createAttendance({
      courseId,
      duration: signInForm.duration,
      location: signInForm.location || undefined,
      classIds: signInForm.classIds.length > 0 ? signInForm.classIds : undefined
    })
    ElMessage.success('签到已发起')
    showSignInDialog.value = false
    signInForm.classIds = []
    signInForm.location = ''
    loadActivities()
  } catch (e: any) {
    ElMessage.error(e.message || '发起签到失败')
  }
}

const handleCreateQuestion = async () => {
  try {
    await createQuestion({
      courseId,
      content: questionForm.content,
      type: questionForm.type,
      options: questionForm.type !== 3 ? questionForm.options.filter(o => o.content) : undefined,
      correctAnswer: questionForm.type !== 3 ? questionForm.correctAnswer : undefined,
      duration: questionForm.duration,
      points: questionForm.points,
      classIds: questionForm.classIds.length > 0 ? questionForm.classIds : undefined
    })
    ElMessage.success('提问已发起')
    showQuestionDialog.value = false
    questionForm.classIds = []
    loadQuestions()
  } catch (e: any) {
    ElMessage.error(e.message || '发起提问失败')
  }
}

const loadVotes = async () => {
  loadingVotes.value = true
  try {
    votes.value = (await getCourseVotes(courseId)) || []
    votes.value.forEach((vote) => {
      voteSelectionsSingle[vote.id] = ''
      voteSelectionsMulti[vote.id] = []
    })
  } catch {
    votes.value = []
  } finally {
    loadingVotes.value = false
  }
}

const handleCreateVote = async () => {
  const title = voteForm.title.trim()
  const options = voteForm.options.map(o => ({ content: (o.content || '').trim() })).filter(o => o.content)
  if (!title) {
    ElMessage.warning('请输入投票主题')
    return
  }
  if (options.length < 2) {
    ElMessage.warning('请至少填写2个选项')
    return
  }
  try {
    await createVote({
      courseId,
      title,
      classIds: voteForm.classIds.length ? voteForm.classIds : undefined,
      type: voteForm.type,
      anonymous: voteForm.anonymous,
      options
    })
    ElMessage.success('投票发起成功')
    showVoteDialog.value = false
    voteForm.title = ''
    voteForm.classIds = []
    voteForm.type = 1
    voteForm.anonymous = true
    voteForm.options = [{ content: '' }, { content: '' }]
    loadVotes()
  } catch (e: any) {
    ElMessage.error(e.message || '发起投票失败')
  }
}

const handleSubmitVote = async (voteId: number) => {
  const vote = votes.value.find(v => v.id === voteId)
  if (!vote) return

  const selected = vote.type === 2
    ? (voteSelectionsMulti[voteId] || [])
    : (voteSelectionsSingle[voteId] || '')
  const isEmpty = Array.isArray(selected) ? selected.length === 0 : !selected
  if (isEmpty) {
    ElMessage.warning('请选择投票选项')
    return
  }
  try {
    await submitVote(voteId, selected)
    ElMessage.success('投票成功')
    loadVotes()
  } catch (e: any) {
    ElMessage.error(e.message || '投票失败')
  }
}

const handleCloseVote = async (voteId: number) => {
  try {
    await closeVote(voteId)
    ElMessage.success('投票已结束')
    loadVotes()
  } catch (e: any) {
    ElMessage.error(e.message || '结束投票失败')
  }
}

const handleCreateHomework = async () => {
  try {
    await createHomework({
      courseId,
      title: homeworkForm.title,
      content: homeworkForm.content,
      chapter: homeworkForm.chapter,
      deadline: homeworkForm.deadline,
      totalPoints: homeworkForm.totalPoints,
      classIds: homeworkForm.classIds.length > 0 ? homeworkForm.classIds : undefined
    })
    ElMessage.success('作业已发布')
    showHomeworkDialog.value = false
    homeworkForm.classIds = []
    homeworkForm.title = ''
    homeworkForm.content = ''
    homeworkForm.chapter = ''
    homeworkForm.deadline = ''
    homeworkForm.totalPoints = 100
    loadHomeworks()
  } catch (e) {
    // error
  }
}

const loadFiles = async () => {
  loadingFiles.value = true
  try {
    files.value = await getCourseFiles(courseId)
  } catch {
    files.value = []
  } finally {
    loadingFiles.value = false
  }
}

const formatFileSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const handlePreviewFile = async (f: any) => {
  try {
    await previewFile(f.id)
  } catch {
    ElMessage.error('预览失败')
  }
}

const handleDownloadFile = async (f: any) => {
  try {
    await downloadFile(f.id, f.fileName)
  } catch {
    ElMessage.error('下载失败')
  }
}

const handleUploadSuccess = () => {
  ElMessage.success('上传成功')
  loadFiles()
}

const handleUploadError = () => {
  ElMessage.error('上传失败')
}
</script>

<style scoped>
.course-detail {
  width: 100%;
  min-height: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.header-info h2 {
  font-size: 28px;
  font-weight: 600;
  color: #3d3225;
  margin: 12px 0 8px;
}

.header-info p {
  color: #8b7355;
  margin: 0;
}

.course-tabs {
  background: white;
  border-radius: 16px;
  padding: 24px;
}

.overview-content {
  display: grid;
  gap: 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  padding: 16px 0;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: #faf8f5;
  border-radius: 12px;
}

.info-item .label {
  color: #8b7355;
  font-size: 14px;
}

.info-item .value {
  color: #3d3225;
  font-size: 16px;
  font-weight: 500;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 20px;
  background: #faf8f5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-item:hover {
  background: #f5f0e8;
  transform: translateY(-2px);
}

.action-item span {
  color: #3d3225;
  font-size: 14px;
}

.tab-content {
  min-height: 300px;
  padding: 16px 0;
}

.toolbar {
  margin-bottom: 16px;
}

.option-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.homework-list {
  display: grid;
  gap: 16px;
}

.homework-card {
  margin-bottom: 0;
}

.homework-card-clickable {
  cursor: pointer;
}

.homework-card-clickable:hover {
  box-shadow: 0 8px 20px rgba(61, 50, 37, 0.08);
}

.homework-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.homework-header h3 {
  margin: 0;
  font-size: 16px;
  color: #3d3225;
}

.homework-content {
  color: #666;
  font-size: 14px;
  margin: 0 0 12px;
}

.homework-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.activity-list {
  display: grid;
  gap: 16px;
}

.activity-card {
  cursor: pointer;
  transition: all 0.2s;
}

.activity-card:hover {
  transform: translateY(-2px);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.activity-header h3 {
  margin: 0;
  font-size: 16px;
  color: #3d3225;
}

.activity-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #3d3225;
}

.stat-label {
  font-size: 12px;
  color: #8b7355;
}

.activity-time {
  font-size: 13px;
  color: #999;
}

.student-activity {
  border-left: 4px solid #409eff;
}

.signin-btn {
  margin-top: 12px;
  width: 100%;
}

.question-list {
  display: grid;
  gap: 16px;
}

.question-card {
  margin-bottom: 0;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.question-time {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

.question-content {
  font-size: 15px;
  color: #303133;
  margin: 0 0 10px;
}

.question-options {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}

.question-option-line {
  color: #606266;
  font-size: 13px;
}

.question-option-label {
  margin-right: 6px;
  font-weight: 600;
}

.student-answer-box {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  display: grid;
  gap: 8px;
}

.student-answer-input {
  margin-bottom: 8px;
}

.student-answer-text {
  margin-left: 8px;
  color: #67c23a;
}

.question-stats {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #909399;
}

.vote-list {
  display: grid;
  gap: 16px;
}

.vote-card {
  margin-bottom: 0;
}

.vote-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.vote-header h3 {
  margin: 0;
  font-size: 16px;
  color: #3d3225;
}

.vote-options {
  display: grid;
  gap: 8px;
}

.vote-option-line {
  padding: 8px 10px;
  border-radius: 8px;
  background: #faf8f5;
}

.vote-option-label {
  color: #303133;
  margin-bottom: 6px;
}

.vote-option-stat {
  display: flex;
  align-items: center;
  gap: 10px;
}

.vote-voter-names {
  margin-top: 6px;
  font-size: 12px;
  color: #8b7355;
}

.vote-option-stat :deep(.el-progress) {
  flex: 1;
}

.vote-footer {
  display: flex;
  gap: 20px;
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.vote-student-box {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  display: grid;
  gap: 8px;
}

.course-file-list {
  display: grid;
  gap: 12px;
}

.course-file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.course-file-main {
  min-width: 0;
  cursor: pointer;
}

.course-file-name {
  color: #3d3225;
  font-weight: 500;
}

.course-file-meta {
  display: flex;
  gap: 16px;
  margin-top: 4px;
  font-size: 12px;
  color: #999;
}
</style>
