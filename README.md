<h1 align="center">Voice Activity Detection</h1>

<br>

<div align="center" style="display: grid; justify-content: center;">

|                                                                  🌟                                                                   |                  Support this project                   |               
|:-------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------:|
|  <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/bitcoin.png" alt="Bitcoin (BTC)" width="32"/>  | <code>bc1qs6qq0fkqqhp4whwq8u8zc5egprakvqxewr5pmx</code> | 
| <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/ethereum.png" alt="Ethereum (ETH)" width="32"/> | <code>0x3147bEE3179Df0f6a0852044BFe3C59086072e12</code> |
|  <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/tether.png" alt="USDT (TRC-20)" width="32"/>   |     <code>TKznmR65yhPt5qmYCML4tNSWFeeUkgYSEV</code>     |

</div>

<br>

<p align="center">JVM library for voice activity detection written in Kotlin based on the C library <a href="https://github.com/dpirch/libfvad">libfvad</a> and ML model <a href="https://github.com/snakers4/silero-vad">Silero</a></p>

### See also

- [Stretch](https://github.com/numq/stretch) *to change the speed of audio without changing the pitch*


- [Speech recognition](https://github.com/numq/speech-recognition) *to transcribe audio to text*


- [Speech generation](https://github.com/numq/speech-generation) *to generate voice audio from text*


- [Text generation](https://github.com/numq/text-generation) *to generate text from prompt*


- [Noise reduction](https://github.com/numq/noise-reduction) *to remove noise from audio*

## When to use

> [!NOTE]
> For best results, it is recommended to apply noise reduction to the input data.

### libfvad

Detects any audio activity, regardless of the sound type. The detection behavior depends on the selected mode. Suitable
for general voice activity detection.

### Silero

Detects voice activity specifically containing human speech. Best for speech-focused tasks like transcription and
voice-controlled systems.

## Features

- Detects voice activity in PCM audio data
- Supports any sampling rate and number of channels due to resampling and downmixing
- Supports different detection modes to balance between sensitivity and accuracy (fvad)

## Installation

- Download latest [release](https://github.com/numq/voice-activity-detection/releases)

- Add library dependency
   ```kotlin
   dependencies {
        implementation(file("/path/to/jar"))
   }
   ```

### libfvad

- Unzip binaries

### Silero

- Add ONNX dependency
   ```kotlin
   dependencies {
        implementation("com.microsoft.onnxruntime:onnxruntime:1.20.0")
   }
   ```

## Usage

> See the [example](example) module for implementation details

### TL;DR

- Call `detect` to process the input data, use `isContinuous = true` with streaming audio

### Step-by-step

- Load binaries if you are going to use fvad
   ```kotlin
   VoiceActivityDetection.Fvad.load(libfvad = "/path/to/libfvad", voiceActivityDetection = "/path/to/voice-activity-detection")
   ```

- Create an instance

  ### fvad

  ```kotlin
  VoiceActivityDetection.Fvad.create()
  ```

  ### Silero

  ```kotlin
  VoiceActivityDetection.Silero.create()
  ```

- Call `inputSizeForMillis` to get the input data size for N milliseconds


- Call `minimumInputSize` to get the audio producer buffer size for real-time detection


- Call `detect` passing the input data, sample rate and number of channels as arguments


- Call `reset` to reset the internal state - for example when the audio source changes


- Call `close` to release resources

## Requirements

- JVM version 9 or higher

## License

This project is licensed under the [Apache License 2.0](LICENSE)

## Acknowledgments

- [libfvad](https://github.com/dpirch/libfvad)
- [Silero](https://github.com/snakers4/silero-vad)
