import { generateService } from '@umijs/openapi'

const SCHEMA_PATH_LOCAL = 'http://localhost:8123'

generateService({
  // 此处可能出错
  requestLibPath: "import request from '@/request'",
  //  填写swagger文档的线上网址
  schemaPath: `${SCHEMA_PATH_LOCAL}/api/v2/api-docs`,
  // 文件生成目录
  serversPath: './src',
})
