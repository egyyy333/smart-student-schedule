// Web Audio API & Speech Synthesis Helpers

let audioCtx: AudioContext | null = null;
let activeOscillators: { osc: OscillatorNode; gain: GainNode }[] = [];
let alarmIntervalId: any = null;

// Initialize Audio Context on demand
function getAudioContext(): AudioContext {
  if (!audioCtx) {
    audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
  }
  if (audioCtx.state === "suspended") {
    audioCtx.resume();
  }
  return audioCtx;
}

// Synthesize a beautiful chime note
function playChimeNote(frequency: number, duration: number, timeOffset: number) {
  try {
    const ctx = getAudioContext();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    osc.type = "sine";
    osc.frequency.setValueAtTime(frequency, ctx.currentTime + timeOffset);
    
    gain.gain.setValueAtTime(0, ctx.currentTime + timeOffset);
    gain.gain.linearRampToValueAtTime(0.15, ctx.currentTime + timeOffset + 0.05);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + timeOffset + duration);

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start(ctx.currentTime + timeOffset);
    osc.stop(ctx.currentTime + timeOffset + duration);

    activeOscillators.push({ osc, gain });
    
    // Clean up finished oscillators
    setTimeout(() => {
      activeOscillators = activeOscillators.filter(item => item.osc !== osc);
    }, (timeOffset + duration + 1) * 1000);
  } catch (error) {
    console.error("Error playing chime note", error);
  }
}

// Play repeating beautiful chime melody for the alarm
export function startAlarmSound() {
  stopAlarmSound();
  
  const ctx = getAudioContext();
  const playMelody = () => {
    // Elegant minor pentatonic melody for a beautiful modern chime
    const melody = [523.25, 587.33, 659.25, 783.99, 880.00, 1046.50]; // C5, D5, E5, G5, A5, C6
    const pattern = [0, 2, 4, 3, 5, 4];
    pattern.forEach((noteIdx, step) => {
      playChimeNote(melody[noteIdx], 1.2, step * 0.35);
    });
  };

  playMelody();
  alarmIntervalId = setInterval(playMelody, 3000);
}

// Stop alarm ringing
export function stopAlarmSound() {
  if (alarmIntervalId) {
    clearInterval(alarmIntervalId);
    alarmIntervalId = null;
  }
  activeOscillators.forEach(item => {
    try {
      item.osc.stop();
      item.gain.disconnect();
    } catch (e) {}
  });
  activeOscillators = [];
}

// Play name pronunciation using SpeechSynthesis
export function speakArabicText(text: string) {
  if (!("speechSynthesis" in window)) {
    console.warn("Speech Synthesis not supported in this browser");
    return;
  }

  // Cancel any ongoing speech
  window.speechSynthesis.cancel();

  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = "ar-SA";
  utterance.rate = 0.9; // Slightly slower for clear Arabic pronunciation
  utterance.pitch = 1.0;

  // Try to find an Arabic voice
  const voices = window.speechSynthesis.getVoices();
  const arabicVoice = voices.find(voice => voice.lang.includes("ar"));
  if (arabicVoice) {
    utterance.voice = arabicVoice;
  }

  window.speechSynthesis.speak(utterance);
}
