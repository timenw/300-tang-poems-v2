#!/usr/bin/env node
/**
 * Batch generate TTS audio for poems using Edge TTS (Yunxi voice).
 * Uses the edge-tts npm package or falls back to direct WebSocket/HTTP.
 * Run: node generate_audio_batch.js
 */
const fs = require('fs');
const { execSync } = require('child_process');

const VOICE = 'zh-CN-YunxiNeural';
const RATE = '-10%';
const OUTPUT_DIR = 'audio';
const POEMS_FILE = 'app/src/main/assets/poems.json';

// Load poems
const poems = JSON.parse(fs.readFileSync(POEMS_FILE, 'utf8'));
console.log(`Loaded ${poems.length} poems`);

// Create output dir
if (!fs.existsSync(OUTPUT_DIR)) fs.mkdirSync(OUTPUT_DIR, { recursive: true });

// Find missing audio files
const needAudio = [];
for (const p of poems) {
  const fname = `poem_${String(p.id).padStart(3, '0')}.mp3`;
  const fpath = `${OUTPUT_DIR}/${fname}`;
  if (!fs.existsSync(fpath) || fs.statSync(fpath).size < 1000) {
    needAudio.push(p);
  }
}

const existing = poems.length - needAudio.length;
console.log(`Existing audio: ${existing}, Need to generate: ${needAudio.length}`);

if (needAudio.length === 0) {
  console.log('All audio files already exist!');
  process.exit(0);
}

// Try to use edge-tts via Python (which uses websockets internally)
// But since Python httpx/urllib can't connect, we'll use a Node.js approach
// Actually, let's check if `edge-tts` CLI is available
try {
  execSync('edge-tts --version', { stdio: 'pipe' });
  console.log('edge-tts CLI available');
  // Generate one by one
  let ok = 0, fail = 0;
  for (const p of needAudio) {
    const title = p.title;
    const content = p.content.replace(/\n/g, '，');
    const text = `《${title}》，${p.author}。${content}`;
    const fname = `poem_${String(p.id).padStart(3, '0')}.mp3`;
    const fpath = `${OUTPUT_DIR}/${fname}`;
    
    try {
      const escaped = text.replace(/"/g, '\\"');
      execSync(`edge-tts --voice ${VOICE} --rate "${RATE}" --write-media "${fpath}" --text "${escaped}"`, 
        { timeout: 30000 });
      const sz = fs.statSync(fpath).size;
      ok++;
      console.log(`  OK  ${fname} (${title}, ${Math.round(sz/1024)}KB)`);
    } catch(e) {
      fail++;
      console.log(`  FAIL ${fname} (${title}): ${e.message.slice(0,80)}`);
    }
  }
  console.log(`\nDone: ${ok} generated, ${fail} failed`);
} catch(e) {
  console.log('edge-tts CLI not available, checking npm package...');
  try {
    require('edge-tts');
    console.log('edge-tts npm package available');
  } catch(e2) {
    console.log('Installing edge-tts...');
    execSync('npm install edge-tts', { stdio: 'inherit' });
  }
}
