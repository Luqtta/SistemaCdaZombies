# CDACustomZombies

Plugin Bukkit 1.6.4 (v1_6_R3) para o modpack **Official Crafting Dead**, que adiciona sistema completo de zumbis customizados com níveis, eventos, recompensas e mecânicas especiais.

## Principais recursos

- **Zumbis por “key”** (`cz_key`): tipos como `CD_ZOMBIE`, `CD_ZOMBIE_FAST`, `CD_ZOMBIE_TANK`, `CD_ZOMBIE_WEAK`, `BOSS`, `CD_ZOMBIE_LENDARIO`, `CD_ZOMBIE_EXPLOSIVO`, `CD_ZOMBIE_FLAMEJANTE`.
- **Níveis (1–4)** por zumbi (`cz_level`) com multiplicadores de vida/dano/velocidade.
- **Spawn por peso** (`SpawnWeights`) com variações raras.
- **Recompensas** por morte (dinheiro e itens com chance).
- **Evento Madrugada do Terror** (horário, multiplicadores e hordas).
- **Combate especial** para Boss/Lendário (knock-up, blind, mensagens).
- **Zumbi Explosivo** com explosão sem quebrar blocos (dano em entidades apenas).
- **Zumbi Flamejante** com dano extra, fogo no player e efeito visual.
- **Scoreboard lateral** mostrando HP do Boss/Lendário para cada jogador que causar dano.

## Compatibilidade

- **Bukkit/Spigot 1.6.4 (v1_6_R3)**
- Dependência obrigatória: **ArzioLib**
- Dependência opcional: **Vault** (economia)

## Instalação

1. Coloque o JAR do plugin em `/plugins`.
2. Garanta a dependência **ArzioLib** instalada.
3. (Opcional) Instale **Vault** + provider de economia.
4. Inicie o servidor para gerar o `config.yml`.
5. Ajuste o `config.yml` conforme necessário.
6. Use `/cz reload` para recarregar.

## Comandos

- `/customzombies reload` (alias: `/cz reload`)
- `/customzombies terror <on|off|auto|status>`

Permissão: `customzombies.admin`

## Configuração

### Zumbis
`Zombies.<KEY>` define nome, vida base, headshot, efeitos e drops.

Exemplo:
```yml
Zombies:
  CD_ZOMBIE_FLAMEJANTE:
    Nome: "&6Zombie Flamejante"
    VidaBase: 28
    HeadShot: true
    Efeitos: ["1,1"]
    DinheiroBase: 12
    Itens:
      - "9934 1 1"
      - "9944 1 1"
      - "9415 1 1"
      - "9373 1 1"
      - "322:1 1 0.2"
```

### Levels
`Levels.<KEY>.<LEVEL>` define multiplicadores e efeitos especiais por nível.

Exemplo:
```yml
Levels:
  CD_ZOMBIE_FLAMEJANTE:
    1: { HealthMultiplier: 1.00, FireTicks: 60, ExtraDamage: 0.5 }
    2: { HealthMultiplier: 1.12, FireTicks: 80, ExtraDamage: 1.0 }
    3: { HealthMultiplier: 1.25, FireTicks: 100, ExtraDamage: 1.5 }
    4: { HealthMultiplier: 1.40, FireTicks: 120, ExtraDamage: 2.0 }
```

### SpawnWeights
Define raridade por tipo base:
```yml
SpawnWeights:
  CD_ZOMBIE:
    CD_ZOMBIE_EXPLOSIVO: 15
    CD_ZOMBIE_FLAMEJANTE: 15
    CD_ZOMBIE: 920
    CD_ZOMBIE_FAST: 55
    CD_ZOMBIE_TANK: 20
    CD_ZOMBIE_WEAK: 5
    BOSS: 2
    CD_ZOMBIE_LENDARIO: 1
```

### Flame Zombie (visual e combate)
```yml
FlameZombie:
  Enabled: true
  Hit:
    ApplyFireAlways: true
    ApplyFireChance: 0.35
  Visual:
    Mode: "EFFECT"        # PACKET | FIRETICKS | EFFECT
    IntervalTicks: 12
```

### Madrugada do Terror
Controla horário, mensagens, som, multiplicadores e hordas.

## Como funciona

- No spawn, o plugin seleciona a `key` via `SpawnWeights` e aplica definição + nível.
- `cz_key` identifica o tipo custom; `cz_level` guarda o nível (1–4).
- No combate, Boss/Lendário aplicam efeitos configurados e exibem mensagens.
- Na morte, o plugin resolve o killer, limpa drops padrões e aplica recompensas.
- O **zumbi explosivo** explode sem quebrar blocos (apenas dano em entidades).
- O **zumbi flamejante** aplica fogo no player e tem efeito visual periódico.

## Scoreboard de Boss/Lendário

Quando o jogador acerta um Boss ou Lendário, aparece um scoreboard lateral com HP atual. Ele some após alguns segundos sem dano ou quando o mob morre.

## Economia

Se o **Vault** estiver presente, o plugin paga dinheiro base e bônus configurados em `Rewards`.

## Licença

Defina a licença desejada para o seu repositório (ex: MIT, GPL, etc.).

---

Qualquer ajuste de balanceamento (vida, dano, drops, chance) é feito diretamente no `config.yml`.
