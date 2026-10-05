# Escopo do código público — base 1.3.18

Revisão de documentação: 2026-10-05. APK de referência: versionCode 1003018,
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

Áudio 1.3.17: Sound nativo preservado em tipos, superclasses e casts. Somente
construções externas estáticas reconhecidas recebem BrowserSound e resolução
de URLs. AudioSession utiliza saída de mídia, SOM salvo, teste PCM e reaplicação
na retomada. Os 15 SWFs Kaguya passam pelo adaptador sem alteração de bytes.
Fixtures e parser independente verificam construções, desvios, switches e
handlers. O usuário confirmou o retorno dos efeitos nessa versão; música segue ausente.

Áudio 1.3.18: resolve URLs sem protocolo do CDN oficial e acrescenta diagnóstico
específico por toque. Metadados HTTP e eventos Sound ficam em dois buffers de
até 64 linhas, sem queries, autoridades de URL, headers ou conteúdo de mídia.
Não há envio periódico à interface. O ajuste ainda não foi confirmado como
solução para a trilha no aparelho.

## Validação

Suítes JVM/Node e inspeções de empacotamento foram executadas na base. Shaders
foram executados no Mesa llvmpipe; isso não é benchmark Android. Há relatos do
usuário de funcionamento do jogo, mas não matriz certificada de aparelhos,
requisitos numéricos de RAM ou garantia de FPS. Consulte README.md para detalhes.
