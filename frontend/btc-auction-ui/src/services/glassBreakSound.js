export function playGlassBreakSound() {
    if (typeof window === "undefined") {
        return;
    }

    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) {
        return;
    }

    let audioContext;
    try {
        audioContext = new AudioContextClass();
    } catch {
        return;
    }

    const now = audioContext.currentTime;
    const masterGain = audioContext.createGain();
    masterGain.gain.setValueAtTime(0.58, now);
    masterGain.connect(audioContext.destination);

    const noiseBuffer = audioContext.createBuffer(1, Math.floor(audioContext.sampleRate * 0.56), audioContext.sampleRate);
    const noiseSamples = noiseBuffer.getChannelData(0);
    let noiseSeed = 0x4d595df4;
    for (let sampleIndex = 0; sampleIndex < noiseSamples.length; sampleIndex += 1) {
        noiseSeed = (noiseSeed * 1664525 + 1013904223) >>> 0;
        noiseSamples[sampleIndex] = (noiseSeed / 0xffffffff) * 2 - 1;
    }

    const noiseSource = audioContext.createBufferSource();
    noiseSource.buffer = noiseBuffer;
    const highPass = audioContext.createBiquadFilter();
    highPass.type = "highpass";
    highPass.frequency.setValueAtTime(740, now);
    const noiseGain = audioContext.createGain();
    noiseGain.gain.setValueAtTime(0.0001, now);
    noiseGain.gain.exponentialRampToValueAtTime(0.3, now + 0.009);
    noiseGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.5);
    noiseSource.connect(highPass);
    highPass.connect(noiseGain);
    noiseGain.connect(masterGain);
    noiseSource.start(now);
    noiseSource.stop(now + 0.52);

    const impact = audioContext.createOscillator();
    impact.type = "triangle";
    impact.frequency.setValueAtTime(240, now);
    impact.frequency.exponentialRampToValueAtTime(48, now + 0.19);
    const impactGain = audioContext.createGain();
    impactGain.gain.setValueAtTime(0.0001, now);
    impactGain.gain.exponentialRampToValueAtTime(0.22, now + 0.006);
    impactGain.gain.exponentialRampToValueAtTime(0.0001, now + 0.22);
    impact.connect(impactGain);
    impactGain.connect(masterGain);
    impact.start(now);
    impact.stop(now + 0.23);

    [
        { frequency: 1840, delay: 0.015, duration: 0.29, volume: 0.1 },
        { frequency: 2670, delay: 0.045, duration: 0.24, volume: 0.075 },
        { frequency: 3520, delay: 0.09, duration: 0.2, volume: 0.055 },
        { frequency: 4980, delay: 0.16, duration: 0.15, volume: 0.035 },
    ].forEach(({ frequency, delay, duration, volume }) => {
        const startTime = now + delay;
        const shardTone = audioContext.createOscillator();
        shardTone.type = "sine";
        shardTone.frequency.setValueAtTime(frequency, startTime);
        shardTone.frequency.exponentialRampToValueAtTime(frequency * 0.72, startTime + duration);
        const shardGain = audioContext.createGain();
        shardGain.gain.setValueAtTime(0.0001, startTime);
        shardGain.gain.exponentialRampToValueAtTime(volume, startTime + 0.006);
        shardGain.gain.exponentialRampToValueAtTime(0.0001, startTime + duration);
        shardTone.connect(shardGain);
        shardGain.connect(masterGain);
        shardTone.start(startTime);
        shardTone.stop(startTime + duration + 0.01);
    });

    audioContext.resume().catch(() => { });
    window.setTimeout(() => {
        audioContext.close().catch(() => { });
    }, 1000);
}