package dev.brahmkshatriya.echo.extension.cineby

object CineByJS {
    const val GET_SERVERS = """async function() {
    const list = await Promise.all(
        window.LIST.map(async item => {
            let result;
            try {
                result = await item.get(DATA);
            } catch (e) {}
            return {
                name: item.name,
                result
            };
        })
    );
    return JSON.stringify(list);
}"""
    const val PAGE_START = """function (){
    const patches = [
        {
            find: "queryKey:[\"WatchMediaSources\"", replacement: [
                {
                    match: /(\w)=({title:.*?}})/,
                    replace: (_, V, data) => `${'$'}{V}=((data)=>globalThis.DATA=data)(${'$'}{data})`
                }
            ],
        },
        {
            find: /,l=async function.*;return l/, replacement: [
                {
                    match: "l=async function(e,t){return(await a(e,t))._data}",
                    replace: "l=async function(e,t){if(e.includes(\"sources-with-title\"))return{e,t};return(await a(e,t))._data}"
                }
            ],
        },
        {
            find: /mediaId cannot be undefined/, replacement: [
                {
                    match: /(async function \w(\(\w,\w,\w\)))({[^}]*?return[^}]*?\})/,
                    replace: (_, fun, param, actual) => `globalThis.DECRYPT=async function ${'$'}{param}${'$'}{actual};${'$'}{fun} { return JSON.stringify({sources:e,subtitles:[]}) }`
                }
            ],
        },
        {
            find: "Original audio", replacement: [
                {
                    match: /sources\.filter\(.{1,50}===\w\.quality\)/g,
                    replace: "sources"
                },
                {
                    match: /(\w)=(\[{name:.*?}\])/,
                    replace: (_, V, list) => `${'$'}{V}=((list)=>globalThis.LIST=list)(${'$'}{list})`
                }
            ],
        },
    ];
    
    /// ======== Do not touch the below! ========
    Object.defineProperty(Function.prototype, "m", {
        set(v) {
            const source = this.toString();
            if (
                source.includes("exports") &&
                (source.includes("false") || source.includes("!1")) &&
                !(Array.isArray(v) && v?.some(m => m.toString().includes("CHROME_WEBSTORE_EXTENSION_ID"))) // react devtools
            ) {
                Object.defineProperty(this, "m", {
                    value: v,
                    configurable: true,
                    enumerable: true,
                    writable: true
                });
    
                patchFactories(v);
    
                delete Function.prototype.m;
                this.m = v;
            } else {
                // huh not webpack_require
                Object.defineProperty(this, "m", {
                  value: v,
                  configurable: true,
                  writable: true,
                  enumerable: true
                });
            }
        },
        configurable: true,
    });
    
    let webpackChunk = [];
    Object.defineProperty(window, "webpackChunk_N_E", {
        configurable: true,
    
        get: () => webpackChunk,
        set: (v) => {
            if (v?.push) {
                if (!v.push.${"$$"}vencordOriginal) {
                    console.log('Patching webpackChunk_N_E.push');
                    patchPush(v);
    
                    delete window.webpackChunk_N_E;
                    window.webpackChunk_N_E = v;
                }
            }
    
            webpackChunk = v;
        }
    });
    
    function patchPush(webpackGlobal) {
        function handlePush(chunk) {
            try {
                patchFactories(chunk[1]);
            } catch (err) {
                console.error("Error in handlePush", err);
            }
    
            return handlePush.${"$$"}vencordOriginal.call(webpackGlobal, chunk);
        }
    
        handlePush.${"$$"}vencordOriginal = webpackGlobal.push;
        handlePush.toString = handlePush.${"$$"}vencordOriginal.toString.bind(handlePush.${"$$"}vencordOriginal);
    
        handlePush.bind = (...args) => handlePush.${"$$"}vencordOriginal.bind(...args);
    
        Object.defineProperty(webpackGlobal, "push", {
            configurable: true,
    
            get: () => handlePush,
            set(v) {
                handlePush.${"$$"}vencordOriginal = v;
            }
        });
    }
    
    function patchFactories(factories) {
        for (const id in factories) {
            let mod = factories[id];
            const originalMod = mod;
    
            const factory = factories[id] = function (module, exports, require) {
                try {
                    mod(module, exports, require);
                } catch (e) {
                    if (mod === originalMod) throw e;
    
                    console.error("Error in patched module", e);
                    return void originalMod(module, exports, require);
                }
    
                exports = module.exports;
                if (!exports) return;
            }
    
            factory.toString = originalMod.toString.bind(originalMod);
            factory.original = originalMod;
    
            let code = "0," + mod.toString();
    
            for (let i=0; i<patches.length; i++) {
                const patch = patches[i];
    
                const moduleMatches = typeof patch.find === "string"
                    ? code.includes(patch.find)
                    : patch.find.test(code);
                if (!moduleMatches) continue;
    
                const previousMod = mod;
                const previousCode = code;
    
                for (const replacement of patch.replacement) {
                    const lastMod = mod;
                    const lastCode = code;
    
                    try {
                        code = code.replace(replacement.match, replacement.replace);
                        mod = (0, eval)(code);
                    } catch (e) {
                        code = lastCode;
                        mod = lastMod;
    
                        console.error("patch failed")
                    }
                }
                patches.splice(i--, 1);
            }
        }
    }
}"""
}