# Geração de quadros — avaliação 1.3.14

Esta versão implementa apenas FSR 1 espacial EASU/RCAS opcional. Não tem geração de quadros ou DLSS.

O novo hook EGL14 permite copiar o framebuffer final GPU→GPU e apresentar a imagem filtrada no mesmo contexto. Cada saída corresponde a uma entrada do AIR; não há imagens intermediárias nem taxa independente de apresentação. O FPS AIR mede callbacks; o estado FSR mede chamadas de swap, sem certificar apresentações físicas únicas.

FSR Frame Generation requer recursos como profundidade, movimento e composição/agendamento. O cliente Flash não fornece esses recursos à ponte; capturar a imagem final não os cria. A geração por fluxo óptico exigiria um novo algoritmo, histórico/sincronização, tratamento de HUD/entrada e testes de artefatos, latência e custo GPU. O hook desta versão é um avanço para pós-processamento espacial, mas não entrega essa geração. A família DLSS usa integrações/hardware NVIDIA RTX, não uma extensão AIR Android.

Fontes oficiais consultadas em 2026-10-04:
- https://gpuopen.com/fidelityfx-superresolution/
- https://gpuopen.com/manuals/fsr_sdk/techniques/frame-interpolation-api/
- https://developer.nvidia.com/rtx/dlss

Nenhum quadro é duplicado para inflar o contador. Não se separou a velocidade de ticks/frame scripts da cadência Stage; animações podem continuar aceleradas em 60 frente ao original30. FSR 1 não corrige isso. Sem benchmark ou teste de integração em um celular; não há garantia de 60/90/120 FPS sustentados.
