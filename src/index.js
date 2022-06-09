const nearley = require('nearley')
const quick = require('./quick.js')

const parser = new nearley.Parser(nearley.Grammar.fromCompiled(quick))

const code =
`function What() {
    function Who() {
    }
}`

parser.feed(code)

function print(target) {
    if (target == null) return ''
    if (typeof target === 'string') return target
    if (Array.isArray(target[0])) return target.map(print).join('')
    const [type, ...data] = target
    return data.map(print).join('')
}

function html(target) {
    return lineify(htmlConverter(target))
}

function htmlConverter(target) {
    if (target == null) return ''
    if (typeof target === 'string') return target
    if (Array.isArray(target[0])) return target.map(htmlConverter).join('')
    const [type, ...data] = target
    if (type === 'keyword') {
        return `<span class="keyword">${data[0]}</span>`
    } else {
        return data.map(htmlConverter).join('')
    }
}

function lineify(html) {
    return `<span class="line">${html.replace(/(\r\n|\r|\n)/gm, '</span><br /><span class="line">')}</span>`
}

console.log(JSON.stringify(parser.results[0]))

