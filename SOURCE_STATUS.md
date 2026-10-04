# Escopo do código público — base 1.3.16

Revisão de documentação: 2026-10-04. APK de referência: versionCode 1003016,
pacote `air.br.davi.narutoair.c71r2`, Android API 24+, ARM64, alvo API 35.

## Publicado

Bootstrap/adaptadores AS3, transporte/cache/adaptação SWF Java, extensões próprias
da ponte Android, entrada/controles, sessão, áudio, compositor GLES experimental,
scripts auxiliares, stubs de teste e testes. Kernels AMD FSR 1 e avisos originais
também publicados. Roteamento Kaguya e manifesto publicados sem mídia externa.

## Dependências ausentes do clone

- `portal.jar` histórico: necessário ao build atual; a fonte original de
  `PortalContext` não está disponível nesta base. Os stubs não o substituem.
- AIR SDK/runtime e Android API jar: dependências externas, sob seus termos.
- Chaves/senha de assinatura: privadas e excluídas.
- Ícones/marca da distribuição e mídia Kaguya: recursos separados do código.
- SWF e conteúdo completo do jogo: pertencem aos titulares; carregados pelo
  cliente através dos serviços utilizados pelo jogo, não publicados aqui.

Portanto, um clone limpo não recompila sozinho todo o APK. A camada própria é
pública sob MIT, mas o produto completo não é um conjunto integralmente aberto.

## Estado funcional

60 FPS é a meta padrão; 90/120/máxima exigem escolha. HUD mede callbacks AIR,
não certifica imagens novas apresentadas. Escala interna pretendida de 80%,
contraste moderado e qualidade medium fazem parte do perfil. FSR 1 opcional,
desligado por padrão, não gera quadros e pode custar desempenho.

Os botões LOG/DIAG da interface foram removidos. O portal tem classificação
limitada de bloqueios e recuperação limitada; não remove regras do site.

Áudio 1.3.16: BrowserSound nativo, resolução de URLs, AudioSession de mídia,
SOM salvo e teste PCM local. Os arquivos Kaguya analisados têm MP3 decodificável
não silencioso; reprodução real Android ainda precisa ser confirmada.

## Validação

Suítes JVM/Node e inspeções de empacotamento foram executadas na base. Shaders
foram executados no Mesa llvmpipe; isso não é benchmark Android. Há relatos do
usuário de funcionamento do jogo, mas não matriz certificada de aparelhos,
requisitos numéricos de RAM ou garantia de FPS. Consulte README.md para detalhes.
