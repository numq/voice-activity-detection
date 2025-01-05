# VAD

**V**oice **A**ctivity **D**etection library for JVM based on the C library [fvad](https://github.com/dpirch/libfvad).

## Features

- Detects voice activity in PCM audio data
- Supports any sampling rate and number of channels due to resampling and downmixing
- Supports different detection modes to balance between sensitivity and accuracy

## Usage

> See the [example](example) module for implementation details

1. Download latest [release](https://github.com/numq/vad/releases)
2. Add library dependency
   ```kotlin
   dependencies {
       implementation(file("/path/to/jar"))
   }
   ```
3. Load binaries
    ```kotlin
    VoiceActivityDetection.load(libfvad = "/path/to/libfvad", libvad = "/path/to/libvad")
    ```
4. Instantiate a library
    ```kotlin
    VoiceActivityDetection.create()
    ```

## Requirements

- JVM version 9 or higher.

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Acknowledgments

- [libfvad](https://github.com/dpirch/libfvad)
