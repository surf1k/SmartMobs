scoreboard players add $tick smoketest 1
execute if score $tick smoketest matches 60 run say [smoketest] summoning vanilla zombies
execute if score $tick smoketest matches 60 at @p run summon minecraft:zombie ^ ^ ^6
execute if score $tick smoketest matches 80 at @p run summon minecraft:zombie ^2 ^ ^6
execute if score $tick smoketest matches 100 at @p run summon minecraft:zombie ^-2 ^ ^6
execute if score $tick smoketest matches 140 run say [smoketest] running the spawnsmart command
execute if score $tick smoketest matches 140 at @p run spawnsmart zombie
execute if score $tick smoketest matches 400 run say [smoketest] 20 seconds of AI ticking done
execute if score $tick smoketest matches 800 run say [smoketest] 40 seconds of AI ticking done