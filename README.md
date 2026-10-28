# FLDSMDFR - Maquina de Comida Infinita (Minecraft 1.8.9 / Forge 11.15.1.2318)

Mod inspirado na maquina FLDSMDFR do filme "Ta Chovendo Hamburguer". Ela comeca como uma capsula branca e laranja
(tela com circulo azul, teclado verde, tira de botoes, pernas e leque de luzes azuis) e evolui sem nivel maximo.

## Como jogar
* Receita: `I G I / R C R / I H I` (I = bloco de ferro, G = vidro, R = redstone, C = bolo, H = funil). No criativo: aba Redstone.
* Clique direito: abre o painel de informacoes. Shift + clique direito com a mao vazia: liga/desliga.
* `/fldsmdfr config [chave valor]`, `/fldsmdfr info`, `/fldsmdfr evolve <expoente>` (teste: evolucao = 10^expoente), `/fldsmdfr surge`.
  Comandos exigem cheats ligados (ou edite `config/fldsmdfr.cfg`).

## Arquitetura (pacote `com.fldsmdfr`)
| Pacote | Responsabilidade |
|---|---|
| `core.MachineState` | Estado persistente + simulacao logica (producao, evolucao, instabilidade, surtos, offline). Sem acesso ao mundo. |
| `core.BigNum` | Numero mantissa x 10^expoente(long): evolucao e producao total nunca estouram (ate 10^(9e18)). |
| `core.StructureLayout` | Mapa deterministico indice -> celula 7x7x7 em camadas concentricas (setores). Bijecao testada. |
| `core.Blueprint` / `ModuleType` | Cada tipo de modulo (camara, tanque, tubo, lancador, turbina, braco, torre, condutor, nucleo auxiliar, plataforma) tem atributos e blocos proprios. Variacoes procedurais pelo indice. |
| `core.MachineStats` | Contagens agregadas e capacidade: `(nucleos*base + camaras*prod) * integracao`; integracao = 1 + 0.30*tanques + 0.20*tubos + 0.25*bracos (razoes limitadas, max x1.75). Modulos decorativos nao produzem. |
| `core.StructureBuilder` | Fila de construcao com orcamento de blocos por update, chunks carregados, estrategia de obstaculos, adiamento/abandono, reparo. |
| `core.LaunchSystem` | Lotes de comida disparados dos lancadores reais, trajetoria balistica, limite de entidades, so perto de jogadores. |
| `tile.TileEntityFldsmdfr` | Orquestra tudo no servidor; cliente so recebe um snapshot pequeno (`MachineView`). |
| `client.RenderCore` | TESR: aparencia da maquina, vibracao, luzes piscando, antenas/tubos/turbinas conforme a evolucao. |
| `gui`, `command`, `config` | Painel, comandos, configuracoes (`config/fldsmdfr.cfg`). |

### Como os tres sistemas se interligam
1. Evolucao `E` cresce exponencialmente; a taxa depende de modulos construidos, capacidade e instabilidade.
2. `E` define o ALVO de modulos (`4 + 2*E^0.45`); o construtor materializa o alvo aos poucos.
3. Somente modulos construidos e funcionais (bloco ancora presente) entram na capacidade de producao e de lancamento.
4. Producao e modulos aumentam a taxa de evolucao e a instabilidade; instabilidade acelera lancamentos, causa surtos de superproducao, faiscas e vibracao.

## Limitacoes (leia)
* Nao compilado/testado dentro do Minecraft por quem escreveu (sem acesso a rede/Forge). Ver `docs/TESTING.md`.
* So a logica independente do Minecraft foi executada e passou (`LogicSelfTest`).
* Altura do mundo e finita: apos 13 camadas (91 blocos) a maquina cresce apenas na horizontal.
* Somente os ultimos `maxTrackedModules` modulos sao verificados/reparados; os mais antigos ficam agregados (assumidos intactos).
* Setores sao as camadas concentricas; nao ha registro persistente por setor alem das contagens globais.
* Quebrar o nucleo apaga os dados da maquina (os blocos construidos permanecem).
* A progressao offline so ocorre quando o chunk da maquina e carregado de novo, limitada por `maxOfflineHours`.
* Conversao de acumulos de comida em blocos NAO foi implementada (apenas limite e remocao dos mais antigos).
* Sem GUI de configuracao in-game: use comando ou arquivo.
* Texturas 16x16 geradas por `tools/generate_textures.py`; modelo da maquina e desenhado por codigo.
