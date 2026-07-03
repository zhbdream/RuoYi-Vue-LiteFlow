/**
 * 简单行级 diff，用于 EL 版本对比
 * @returns {{ left: Array<{line, type, text}>, right: Array<{line, type, text}> }}
 */
export function diffLines(leftText, rightText) {
  const leftLines = splitLines(leftText)
  const rightLines = splitLines(rightText)
  const m = leftLines.length
  const n = rightLines.length
  const dp = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0))

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (leftLines[i - 1] === rightLines[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }

  const left = []
  const right = []
  let i = m
  let j = n
  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && leftLines[i - 1] === rightLines[j - 1]) {
      left.unshift({ line: i, type: 'same', text: leftLines[i - 1] })
      right.unshift({ line: j, type: 'same', text: rightLines[j - 1] })
      i--
      j--
    } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
      right.unshift({ line: j, type: 'add', text: rightLines[j - 1] })
      left.unshift({ line: null, type: 'empty', text: '' })
      j--
    } else {
      left.unshift({ line: i, type: 'remove', text: leftLines[i - 1] })
      right.unshift({ line: null, type: 'empty', text: '' })
      i--
    }
  }
  return { left, right }
}

function splitLines(text) {
  if (!text) {
    return ['']
  }
  return String(text).replace(/\r\n/g, '\n').split('\n')
}

export function hasDiff(leftText, rightText) {
  return splitLines(leftText).join('\n') !== splitLines(rightText).join('\n')
}
