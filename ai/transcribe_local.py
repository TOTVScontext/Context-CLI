import argparse
from pathlib import Path

from faster_whisper import WhisperModel


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("audio", help="Caminho do arquivo WAV")
    parser.add_argument(
        "--model",
        default="base",
        choices=["tiny", "base", "small"],
        help="Tamanho do modelo local",
    )
    args = parser.parse_args()

    audio_path = Path(args.audio)

    if not audio_path.exists():
        raise FileNotFoundError(
            f"Arquivo de audio nao encontrado: {audio_path}"
        )

    print(f"Carregando Whisper local: {args.model}")

    model = WhisperModel(
        args.model,
        device="cpu",
        compute_type="int8",
    )

    segments, info = model.transcribe(
        str(audio_path),
        language="pt",
        beam_size=5,
        condition_on_previous_text=False,
        vad_filter=True,
        vad_parameters={
            "min_silence_duration_ms": 500,
        },
    )


    texto = " ".join(segment.text.strip() for segment in segments).strip()

    print(f"Idioma detectado: {info.language}")
    print("Transcricao local:")
    print(texto)


if __name__ == "__main__":
    main()
