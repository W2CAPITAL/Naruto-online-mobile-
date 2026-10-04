# Magpie — avaliação 1.3.14

Blinue/Magpie é um aplicativo Windows/DirectX. Seus efeitos FSR_EASU/FSR_RCAS fazem ampliação e nitidez; a FAQ oficial não prevê geração de quadros. Não foi embutido o executável/código Windows no Android.

A 1.3.14 implementa wrappers GLES3 dos kernels FP32 FSR 1 oficiais da AMD e um hook de apresentação AIR EGL14. Essa integração experimental oferece filtros espaciais opcionais, sem interpolação, DLSS ou simulação adicional. A cópia de framebuffer e filtros são GPU→GPU; a saída permanece ligada ao swap AIR. Sem CPU screenshots. Kaguya parcial continua disponível.

Shaders validados em Mesa llvmpipe; integração e desempenho não foram testados em Android. Padrão 60, FSR desligado; taxas superiores apenas por escolha. FPS AIR não equivale a apresentações físicas únicas e a opção FSR pode reduzir FPS. Veja README.md e FRAME_GENERATION_STATUS.md.

Fontes consultadas em 2026-10-04:
- https://github.com/Blinue/Magpie
- https://github.com/Blinue/Magpie/wiki/FAQ-(EN)
- https://github.com/Blinue/Magpie/wiki/Built-in%20effects
- https://github.com/GPUOpen-Effects/FidelityFX-FSR
