import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import vm from 'node:vm';
import assert from 'node:assert/strict';
// Exercise the production merge with AS3 annotations removed; not an AIR test.
const source=readFileSync(fileURLToPath(new URL('../src/br/davi/narutoair/compat/BrowserLaunchParameters.as',import.meta.url)),'utf8');
const method=source.slice(source.indexOf('public static function merge('),source.lastIndexOf('\n    }'))
    .replace('public static function','function')
    .replace(/\b(var\s+\w+|\w+(?=:\w+(?:[,)]))):(?:String|Object|Array|int|Error|void)/g,'$1')
    .replace(/\)\s*:Object\s*\{/,') {')
    .replace(/for each\(var (\w+) in (\w+)\)/g,'for(var $1 of $2)');
const context=vm.createContext({});vm.runInContext(method,context);
const merge=(url,vars)=>JSON.parse(JSON.stringify(context.merge(url,vars)));
// Regression: loadBytes previously discarded the complete URL query at the root.
assert.deepEqual(merge('https://cdn.test/entry.swf?server=878&token=a%2Bb&v=42',{}),{server:'878',token:'a+b',v:'42'});
assert.deepEqual(merge('https://cdn.test/entry.swf?server=878&token=query',{token:'html',debug:false,count:7}),{server:'878',token:'html',debug:'false',count:'7'});
assert.deepEqual(merge('entry.swf?text=ol%C3%A1+vila&empty=&flag&same=first&same=last#ignored=1',null),{text:'olá vila',empty:'',flag:'',same:'last'});
assert.deepEqual(merge('entry.swf?bad=%ZZ&good=a%3Db&%ZZ=x',{nil:null}),{good:'a=b',nil:''});
assert.deepEqual(merge('entry.swf#fake?token=x',{a:'b'}),{a:'b'});
assert.deepEqual(merge('entry.swf?__proto__=x&constructor=y&prototype=z&normal=ok',JSON.parse('{"__proto__":"bad","constructor":"bad","prototype":"bad"}')),{normal:'ok'});
assert.deepEqual(merge('entry.swf',null),{});
console.log('PASS: production AS3 launch parameters (type-erased): URL query preserved for loadBytes, FlashVars precedence, decoding, empty values, malformed pairs, fragments and prototype keys. AIR runtime not exercised.');
