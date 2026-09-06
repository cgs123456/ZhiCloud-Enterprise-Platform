import {dirname, relative, resolve} from 'path'
import type {ConfigEnv, UserConfig} from 'vite'
import {loadEnv, normalizePath} from 'vite'
import {createVitePlugins} from './build/vite'
import {exclude, include} from "./build/vite/optimize"
// 当前执行node命令时文件夹的地址(工作目录)
const root = process.cwd()

// 路径查找
function pathResolve(dir: string) {
    return resolve(root, '.', dir)
}

function getRelativeScssUsePath(filename: string, targetPath: string) {
    const cleanFilename = filename.split('?')[0]
    const relativePath = normalizePath(relative(dirname(cleanFilename), targetPath))
    return relativePath.startsWith('.') ? relativePath : `./${relativePath}`
}

// https://vitejs.dev/config/
export default ({command, mode}: ConfigEnv): UserConfig => {
    let env = {} as any
    const isBuild = command === 'build'
    if (!isBuild) {
        env = loadEnv((process.argv[3] === '--mode' ? process.argv[4] : process.argv[3]), root)
    } else {
        env = loadEnv(mode, root)
    }
    const variablesScssPath = pathResolve('src/styles/variables.scss')
    return {
        base: env.VITE_BASE_PATH,
        root: root,
        // 服务端渲染
        server: {
            port: env.VITE_PORT, // 端口号
            host: "0.0.0.0",
            open: env.VITE_OPEN === 'true',
            // 本地跨域代理：浏览器同源请求 vite，由 vite 转发到后端（后端 CORS 白名单不含无端口 Origin 时必须走此模式）
            proxy: {
                "/admin-api": {
                    target: "http://127.0.0.1:48080",
                    ws: true,
                    changeOrigin: true,
                },
                "/druid": {
                    target: "http://127.0.0.1:48080",
                    changeOrigin: true,
                },
            },
        },
        // 项目使用的vite插件。 单独提取到build/vite/plugin中管理
        plugins: createVitePlugins(isBuild, env),
        css: {
            lightningcss: {
                // Preserve legacy star-hack declarations by stripping invalid syntax during minification.
                errorRecovery: true
            },
            preprocessorOptions: {
                scss: {
                    additionalData: (source: string, filename: string) => {
                        const normalizedFilename = normalizePath(filename)
                        // Windows 下更容易触发重复注入：定义或显式转导变量的文件，不能再次注入同一个
                        // `@use ... as *`，否则 Sass 会报 duplicate global variables。
                        if (
                            normalizedFilename.endsWith('/src/styles/variables.scss') ||
                            normalizedFilename.endsWith('/src/styles/global.module.scss')
                        ) {
                            return source
                        }
                        return `@use "${getRelativeScssUsePath(filename, variablesScssPath)}" as *;\n${source}`
                    },
                    api: 'modern-compiler'
                }
            }
        },
        resolve: {
            extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.scss', '.css'],
            alias: [
                {
                    find: /\@\//,
                    replacement: `${pathResolve('src')}/`
                }
            ]
        },
        build: {
            chunkSizeWarningLimit: 1500,
            minify: 'oxc',
            outDir: env.VITE_OUT_DIR || 'dist',
            reportCompressedSize: false,
            sourcemap: env.VITE_SOURCEMAP === 'true' ? 'hidden' : false,
            rollupOptions: {
                output: {
                    minify: {
                        compress: {
                            dropDebugger: env.VITE_DROP_DEBUGGER === 'true',
                            dropConsole: env.VITE_DROP_CONSOLE === 'true'
                        }
                    },
                    // 公网托管（HSK file-hosting）有 2000 文件数上限；原 codeSplitting.groups + 路由级
                    // 动态导入会产出 2000+ 个 chunk。改为内联所有动态导入为单个入口包，文件数骤降到个位数，
                    // 同时保留 gzip 压缩选项由 VITE_COMPRESS 控制。代价：首屏加载单个较大 JS（懒加载失效）。
                    inlineDynamicImports: true,
                },
            },
        },
        optimizeDeps: {include, exclude}
    }
}
