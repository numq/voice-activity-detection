# Voice Activity Detection

JVM library for voice activity detection written in kotlin based on C library [fvad](https://github.com/dpirch/libfvad)
and [Silero](https://github.com/snakers4/silero-vad)

## Features

- Detects voice activity in PCM audio data
- Supports any sampling rate and number of channels due to resampling and downmixing
- Supports different detection modes to balance between sensitivity and accuracy (fvad)

## Usage

> See the [example](example) module for implementation details

1. Download latest [release](https://github.com/numq/vad/releases)
2. Add library dependency
   ```kotlin
   dependencies {
       implementation(file("/path/to/jar"))
   }
   ```
3. Load binaries if you are going to use fvad
   ```kotlin
   VoiceActivityDetection.Fvad.load(libfvad = "/path/to/libfvad", libvad = "/path/to/libvad")
   ```
4. Instantiate a library
    - fvad
   ```kotlin
   VoiceActivityDetection.Fvad.create()
   ```
    - Silero
   ```kotlin
   VoiceActivityDetection.Silero.create()
   ```

## Requirements

- JVM version 9 or higher.

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Acknowledgments

- [fvad](https://github.com/dpirch/libfvad)
- [Silero](https://github.com/snakers4/silero-vad)
