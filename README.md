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

<img width="641" height="469" alt="image" src="https://github.com/user-attachments/assets/c9037d39-d7c3-445e-bace-9df345b27dac" />

<img width="499" height="459" alt="image" src="https://github.com/user-attachments/assets/d1affb3f-aacb-4268-a5b1-9fddcc0adaa1" />

<img width="894" height="602" alt="image" src="https://github.com/user-attachments/assets/a7a96433-60d0-4cab-a6c4-500be59e6f0a" />

<img width="889" height="476" alt="image" src="https://github.com/user-attachments/assets/c6105f26-70a5-46e3-9ae1-88511b55f628" />

<img width="762" height="553" alt="image" src="https://github.com/user-attachments/assets/e543c70e-7fac-4551-a8f6-2587cb18d270" />

<img width="280" height="325" alt="image" src="https://github.com/user-attachments/assets/83954e73-ad72-4d66-be1f-7082dc792f36" />

<img width="491" height="382" alt="image" src="https://github.com/user-attachments/assets/8d0f1fac-bb9a-4841-881f-f4bd639323d3" />



