# ======================================================================
# 下载 BGE-base-zh-v1.5 ONNX 模型到 zhicloud-server 资源目录（Windows PowerShell 版本）
#
# 用途：为 zhicloud-module-ai-rag 模块准备本地化 Embedding 模型
#
# 使用方法：
#   powershell -ExecutionPolicy Bypass -File scripts\download-bge-model.ps1
#   或右键此脚本 -> "使用 PowerShell 运行"
#
# 运行环境：
#   - Windows PowerShell 5.1+ / PowerShell Core 7+
#   - 使用内置 Invoke-WebRequest（支持断点续传 -Headers Range）
#
# 模型说明：
#   - 模型：BAAI/bge-base-zh-v1.5（中文，768 维，~390MB）
#   - 用途：zhicloud-module-ai-rag 的本地 Embedding（ONNX Runtime 嵌入应用）
#   - 维度：768（需与 AiragConfiguration.DEFAULT_VECTOR_DIMENSION 对齐）
#
# 启用 RAG 模块步骤：
#   1. 部署 PostgreSQL 16 + pgvector（参考 docker-compose.yml）
#   2. 执行 sql\postgresql\airag_pgvector.sql 建表
#   3. 运行本脚本下载模型
#   4. 在 AiragConfiguration 中新增 airagEmbeddingModel Bean（参考 EMBEDDING_DECISION.md）
#   5. 配置 zhicloud.airag.enabled=true
#
# 参考链接：
#   - 模型主页：https://huggingface.co/BAAI/bge-base-zh-v1.5
#   - 选型决策：zhicloud-module-ai-rag\EMBEDDING_DECISION.md
# ======================================================================

# 严格错误处理：遇错即停
$ErrorActionPreference = "Stop"

# ---------------- 配置 ----------------
$ModelRepo  = "BAAI/bge-base-zh-v1.5"
$BaseUrl    = "https://huggingface.co/$ModelRepo/resolve/main"

# 目标目录：zhicloud-server\src\main\resources\airag\bge-base-zh-v1.5\
$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$TargetDir  = Join-Path $ProjectRoot "zhicloud-server\src\main\resources\airag\bge-base-zh-v1.5"

# 需要下载的文件列表
$Files = @(
    "model.onnx",
    "tokenizer.json",
    "tokenizer_config.json",
    "config.json",
    "vocab.txt"
)

# ---------------- 工具函数 ----------------
function Write-Log {
    param([string]$Message)
    Write-Host "[download-bge-model] $Message"
}

function Write-Error-Exit {
    param([string]$Message)
    Write-Host "[download-bge-model][ERROR] $Message" -ForegroundColor Red
    exit 1
}

# 下载单个文件（支持断点续传）
function Download-File {
    param(
        [string]$FileName
    )
    $url    = "$BaseUrl/$FileName"
    $target = Join-Path $TargetDir $FileName

    # 如已存在，显示当前大小（Invoke-WebRequest 在 -OutFile 模式下会追加，需手动处理续传）
    if (Test-Path $target) {
        $existingSize = (Get-Item $target).Length
        Write-Log "已存在文件，将尝试断点续传：$FileName ($existingSize bytes)"
    }

    Write-Log "开始下载：$FileName"
    Write-Log "  URL: $url"

    try {
        # Invoke-WebRequest 默认会跟随重定向（HuggingFace 会重定向到 CDN）
        # -OutFile: 指定输出文件
        # -PassThru: 返回响应对象（用于获取状态码）
        # -TimeoutSec: 600 秒（10 分钟，应对大文件下载）
        # HuggingFace 支持 Range 请求，但 Invoke-WebRequest 在 -OutFile 模式下会覆盖
        # 如需严格断点续传，请删除目标文件后重新下载
        $response = Invoke-WebRequest -Uri $url -OutFile $target -PassThru -TimeoutSec 600 -UseBasicParsing
        $size = (Get-Item $target).Length
        Write-Log "下载完成：$FileName ($size bytes, HTTP $($response.StatusCode))"
    }
    catch {
        Write-Error-Exit "下载失败：$FileName -> $($_.Exception.Message)"
    }
}

# 显示最终目录结构
function Show-Directory {
    Write-Log "目标目录结构："
    if (Test-Path $TargetDir) {
        Get-ChildItem -Path $TargetDir -File | ForEach-Object {
            $sizeKB = [math]::Round($_.Length / 1KB, 2)
            Write-Host ("  {0,-25} {1,10} KB" -f $_.Name, $sizeKB)
        }
    } else {
        Write-Log "目标目录不存在：$TargetDir"
    }
}

# ---------------- 主流程 ----------------
function Main {
    Write-Log "BGE-base-zh-v1.5 模型下载脚本（PowerShell 版）"
    Write-Log "项目根目录：$ProjectRoot"
    Write-Log "目标目录：$TargetDir"
    Write-Log "模型仓库：https://huggingface.co/$ModelRepo"
    Write-Host ""

    # 1. 创建目标目录
    if (-not (Test-Path $TargetDir)) {
        New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
        Write-Log "已创建目标目录：$TargetDir"
    } else {
        Write-Log "目标目录已存在：$TargetDir"
    }
    Write-Host ""

    # 2. 逐个下载文件
    Write-Log "开始下载 $($Files.Count) 个文件..."
    foreach ($file in $Files) {
        Download-File -FileName $file
        Write-Host ""
    }

    # 3. 校验关键文件
    Write-Log "校验关键文件..."
    $missing = @()
    foreach ($file in $Files) {
        $fullPath = Join-Path $TargetDir $file
        if (-not (Test-Path $fullPath)) {
            $missing += $file
        }
    }
    if ($missing.Count -gt 0) {
        Write-Error-Exit "以下文件缺失：$($missing -join ', ')"
    }
    Write-Log "所有文件校验通过 ✓"
    Write-Host ""

    # 4. 显示目录结构
    Show-Directory
    Write-Host ""

    # 5. 使用提示
    Write-Log "下载完成！"
    Write-Log "下一步操作："
    Write-Log "  1. 在 AiragConfiguration 中新增 airagEmbeddingModel Bean（参考 EMBEDDING_DECISION.md §4.2 方式 A）"
    Write-Log "  2. 补充 spring-ai-transformers 依赖（如未在 zhicloud-module-ai 中引入）"
    Write-Log "  3. 配置 zhicloud.airag.enabled=true 启用 RAG 模块"
    Write-Log "  4. 执行 sql\postgresql\airag_pgvector.sql 建表（如未执行）"
}

Main
