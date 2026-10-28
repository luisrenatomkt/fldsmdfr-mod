# Plano de testes

Legenda: [AUTO] ja executado em Java puro (`LogicSelfTest`), [MANUAL] precisa do Minecraft.

Teste automatico (sem Minecraft): veja o topo do README do GitHub Actions ou rode
`javac` nas classes `core.{Hash,BigNum,FoodRegistry,ModuleType,StructureLayout,MachineStats}` com um stub de
`net.minecraft.nbt.NBTTagCompound` e execute `com.fldsmdfr.LogicSelfTest`. Resultado atual: ALL TESTS PASSED.

| # | Teste | Como verificar |
|---|---|---|
| 1 | Instalacao/inicializacao [MANUAL] | Colocar o jar em `mods/`, abrir com Forge 1.8.9; mod "FLDSMDFR" na lista, sem crash. |
| 2 | Colocacao [MANUAL] | Craftar/pegar o bloco; colocar; a capsula aparece virada para voce. |
| 3 | Funcionamento inicial [MANUAL] | Painel abre; estado "Operacional"; luzes pulsando. |
| 4 | Producao de comida [MANUAL] | Em poucos segundos hamburgueres caem ao redor (perto do jogador). |
| 5 | Construcao de modulos [MANUAL] | `/fldsmdfr config expansionSpeed 5`: modulos aparecem em celulas de 7 blocos. |
| 6 | Producao sobe com modulos [AUTO+MANUAL] | Auto: capacidade 3.0 -> 11.0 -> 18.15. Manual: "Capacidade estimada" no painel sobe. |
| 7 | Instabilidade [MANUAL] | `instabilityRate 50`: barra sobe, faiscas, luzes piscam. |
| 8 | Varios lancadores [MANUAL] | Com varios lancadores prontos, comida sai de varias torres. |
| 9 | Expansao horizontal/vertical [AUTO+MANUAL] | Auto: camadas ate VMAX=12. Manual: `evolve 8`. |
| 10 | Salvar/carregar [MANUAL] | Sair e reentrar: tempo, evolucao e modulos mantidos. |
| 11 | Progressao offline [MANUAL] | Fechar 5+ min, reabrir: evolucao maior; blocos sao construidos aos poucos. |
| 12 | Setores nao carregados [MANUAL] | Afastar-se: modulos em chunks nao carregados ficam adiados e sao construidos depois. |
| 13 | Sem duplicatas [AUTO+MANUAL] | Auto: 400000 indices sem celula repetida. Manual: nenhum modulo sobreposto. |
| 14 | Limite de entidades [MANUAL] | `stormIntensity 100`: `maxActiveFood` nunca e excedido (F3 contador de entidades). |
| 15 | Valores muito altos [AUTO+MANUAL] | Auto: 10^10000 sem overflow. Manual: `/fldsmdfr evolve 400`, `info` mostra notacao cientifica. |
| 16 | Funcionamento prolongado [MANUAL] | Deixar 1 h com `evolutionSpeed 100`; sem NaN no painel, TPS estavel. |
| 17 | Reinicio do jogo [MANUAL] | Fechar o Minecraft e abrir: estado recuperado. |
