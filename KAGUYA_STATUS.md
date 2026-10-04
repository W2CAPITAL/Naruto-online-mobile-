# Kaguya — port parcial 1.3.12

Pacote de origem: Naruto_Kaguya_Remaster_Portable.zip, fornecido anteriormente pelo usuário. Autor indicado nas instruções: Herrington Kaguya. O ReadMe original acompanha os recursos; declara o mod gratuito e proíbe obter benefícios com o software. Nenhum executável Windows foi executado ou incluído no APK.

## Incluído e roteado no APK

- Cinco estilos cosméticos VENTO/FOGO/RAIO/ÁGUA/TERRA, cada um com dois retratos PNG: 176×68 e 45×45. Os nomes vêm das pastas feng/huo/lei/shui/tu. O estilo é escolhido pelo usuário, independentemente da classe do protagonista.
- Substituição limitada a retratos da aparência 1 do protagonista em IDs 10000101 a 10000501, nas duas pastas head conhecidas. O caminho 10000201/1.png foi observado no log do usuário; não existe um mapa original validado de cada ID para cada estilo.
- Quinze SWF de som, exportação s1: s1754, s1790, s1791, s1792, s17661 a s17670 e s17676. Cada arquivo só substitui o mesmo nome em assets/sound/swf quando solicitado pelo cliente.
- ON/OFF e estilo salvos localmente. Desligar conserva o cliente original. Recursos inválidos/ausentes retornam ao transporte oficial; consultas de outros hosts, variantes, IDs, configurações e caminhos codificados não são substituídas.

A origem e SHA-256 de cada um dos 25 arquivos constam em mods/kaguya/manifest.json. Os PNG tiveram dimensões e CRCs conferidos; SWFs tiveram comprimento, tags e exportação de som conferidos. Testes de transporte HTTP verificam os bytes servidos pelo APK-modelo, os cinco estilos, HEAD e retorno ao cache original. Não houve verificação do visual/som dentro de um Android nesta validação.

## Não incluído

O pacote original usa KAGUYA_CIENT.exe/SASORI.exe e infraestrutura Windows sem código-fonte nem mapa de IDs disponível. Seus outros 12 SWF incluem classes/interfaces/animações antigas que não foram substituídas no cliente BR atual. Imagens 300×410/110×100/812×583, imagens numéricas de categoria desconhecida, animações do personagem e interfaces não foram portadas. As pastas grandes de BGM/Animation effect já faltavam no pacote recebido. Este port não altera combate, inventário, servidor, conta ou autenticação; não entrega o remaster completo.
