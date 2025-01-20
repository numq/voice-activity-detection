# Voice Activity Detection

JVM library for voice activity detection written in Kotlin based on the C
library [libfvad](https://github.com/dpirch/libfvad) and based on the ML
model [Silero](https://github.com/snakers4/silero-vad)

## Features

- Detects voice activity in PCM audio data
- Supports any sampling rate and number of channels due to resampling and downmixing
- Supports different detection modes to balance between sensitivity and accuracy (fvad)

## Installation

- Download latest [release](https://github.com/numq/vad/releases)

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

> [!IMPORTANT]
> If speech is not recognized, the reason may be that the input data size is too small

### fvad

- Load binaries if you are going to use fvad
   ```kotlin
   VoiceActivityDetection.Fvad.load(libfvad = "/path/to/libfvad", libvad = "/path/to/libvad")
   ```

- Instantiate a library
   ```kotlin
   VoiceActivityDetection.Fvad.create()
   ```

### Silero

- Instantiate a library
   ```kotlin
   VoiceActivityDetection.Silero.create()
   ```

## Requirements

- JVM version 9 or higher.

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Acknowledgments

- [libfvad](https://github.com/dpirch/libfvad)
- [Silero](https://github.com/snakers4/silero-vad)
