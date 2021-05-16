package by.jackraidenph.dragonsurvival.handlers;

import by.jackraidenph.dragonsurvival.DragonSurvivalMod;
import by.jackraidenph.dragonsurvival.capability.DragonStateHandler;
import by.jackraidenph.dragonsurvival.capability.DragonStateProvider;
import by.jackraidenph.dragonsurvival.entity.MagicalPredatorEntity;
import by.jackraidenph.dragonsurvival.nest.NestEntity;
import by.jackraidenph.dragonsurvival.network.DiggingStatus;
import by.jackraidenph.dragonsurvival.network.PacketSyncCapabilityMovement;
import by.jackraidenph.dragonsurvival.network.RefreshDragons;
import by.jackraidenph.dragonsurvival.network.StartJump;
import by.jackraidenph.dragonsurvival.network.SyncCapabilityDebuff;
import by.jackraidenph.dragonsurvival.network.SynchronizeDragonCap;
import by.jackraidenph.dragonsurvival.util.ConfigurationHandler;
import by.jackraidenph.dragonsurvival.util.DamageSources;
import by.jackraidenph.dragonsurvival.util.DragonLevel;
import by.jackraidenph.dragonsurvival.util.DragonType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.HorseEntity;
import net.minecraft.entity.passive.horse.SkeletonHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;

import java.lang.reflect.Field;
import java.util.List;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber
public class EventHandler {

    @SubscribeEvent
    public static void onDamage(LivingAttackEvent event) {
        LivingEntity livingEntity = event.getEntityLiving();
        DamageSource damageSource = event.getSource();
        DragonStateProvider.getCap(livingEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                if (damageSource.isFire() && dragonStateHandler.getType() == DragonType.CAVE)
                    	event.setCanceled(true);
                else if (damageSource == DamageSource.SWEET_BERRY_BUSH && dragonStateHandler.getType() == DragonType.FOREST)
                	event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent playerTickEvent) {
		if (playerTickEvent.phase != TickEvent.Phase.START)
			return;
        PlayerEntity playerEntity = playerTickEvent.player;
        DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                for (int i = 0; i < playerEntity.inventory.getContainerSize(); i++) {
                    ItemStack stack = playerEntity.inventory.getItem(i);
                    Item item = stack.getItem();
                    if (item instanceof CrossbowItem || item instanceof BowItem || item instanceof ShieldItem) {
                        playerEntity.drop(playerEntity.inventory.removeItemNoUpdate(i), true, false);
                    }
                }
                if (playerEntity instanceof ServerPlayerEntity) {
                    PlayerInteractionManager interactionManager = ((ServerPlayerEntity) playerEntity).gameMode;
                    Field field = PlayerInteractionManager.class.getDeclaredFields()[5];
                    field.setAccessible(true);
                    if (field.getType() == boolean.class) {
                        try {
                            boolean isMining = field.getBoolean(interactionManager);
                            DragonSurvivalMod.CHANNEL.send(PacketDistributor.ALL.noArg(), new DiggingStatus(playerEntity.getId(), isMining));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //traits
                // TODO: Clean this up, lots of extra math is being run on the client when only the server needs to be running it.
                {
                    World world = playerEntity.level;
                    BlockState blockUnder = world.getBlockState(playerEntity.blockPosition().below());
                    Block block = blockUnder.getBlock();
                    switch (dragonStateHandler.getType()) {
                        case CAVE:
                            if (!world.isClientSide && block.is(BlockTags.BASE_STONE_NETHER) || block.is(BlockTags.BASE_STONE_OVERWORLD)
                                    || block.is(BlockTags.STONE_BRICKS) || block.is(Blocks.NETHER_GOLD_ORE) || block.is(BlockTags.BEACON_BASE_BLOCKS))
                                    playerEntity.addEffect(new EffectInstance(Effects.MOVEMENT_SPEED, 65, 1, false, false));
                            if (ConfigurationHandler.NetworkedConfig.getEnableDragonDebuffs() && !playerEntity.isCreative() && (playerEntity.isInWaterOrBubble() || playerEntity.isInWaterOrRain())) {
                            	if (playerEntity.tickCount % 10 == 0)
                            		playerEntity.hurt(DamageSources.WATER_BURN, 1);
                            	if (playerEntity.tickCount % 5 == 0)
                            		playerEntity.playSound(SoundEvents.LAVA_EXTINGUISH, 1.0F, (playerEntity.getRandom().nextFloat() - playerEntity.getRandom().nextFloat()) * 0.2F + 1.0F);
                                if (world.isClientSide)
                                	world.addParticle(ParticleTypes.POOF, 
                                			playerEntity.getX() + world.random.nextDouble() *  (world.random.nextBoolean() ? 1 : -1), 
                                			playerEntity.getY() + 0.5F,
                                			playerEntity.getZ() + world.random.nextDouble() *  (world.random.nextBoolean() ? 1 : -1), 
                                			0, 0, 0);
                            }
                            if (playerEntity.isOnFire())
                                playerEntity.clearFire();
                            break;
                        case FOREST:
                            if (!world.isClientSide && block.is(BlockTags.LOGS) || block.is(BlockTags.LEAVES) || block.is(BlockTags.PLANKS)
                                    || block.is(Tags.Blocks.DIRT))
                                    playerEntity.addEffect(new EffectInstance(Effects.MOVEMENT_SPEED, 65, 1, false, false));
                            if (!world.isClientSide && ConfigurationHandler.NetworkedConfig.getEnableDragonDebuffs() && !playerEntity.isCreative()) {
                                WorldLightManager lightManager = world.getChunkSource().getLightEngine();
                                if ((lightManager.getLayerListener(LightType.BLOCK).getLightValue(playerEntity.blockPosition()) < 3 && lightManager.getLayerListener(LightType.SKY).getLightValue(playerEntity.blockPosition()) < 3)) {
                            		dragonStateHandler.getDebuffData().timeInDarkness++;
                            		if (dragonStateHandler.getDebuffData().timeInDarkness == 1 || dragonStateHandler.getDebuffData().timeInDarkness == 20 * 10)
                            			DragonSurvivalMod.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> playerEntity), new SyncCapabilityDebuff(playerEntity.getId(), dragonStateHandler.getDebuffData().timeWithoutWater, dragonStateHandler.getDebuffData().timeInDarkness));
                            		else if (dragonStateHandler.getDebuffData().timeInDarkness > 20 * 10)
                                        playerEntity.addEffect(new EffectInstance(DragonEffects.STRESS, 20 * 10 + 5));
                                } else if (dragonStateHandler.getDebuffData().timeInDarkness > 0) {
                            		dragonStateHandler.getDebuffData().timeInDarkness = 0;
                            		DragonSurvivalMod.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> playerEntity), new SyncCapabilityDebuff(playerEntity.getId(), dragonStateHandler.getDebuffData().timeWithoutWater, dragonStateHandler.getDebuffData().timeInDarkness));
                                	}
                        		}
                            break;
                        case SEA:
                            if (!world.isClientSide && block.is(BlockTags.IMPERMEABLE) || block.is(BlockTags.ICE) || block.is(BlockTags.SAND)
                                    || block.is(BlockTags.CORAL_BLOCKS))
                                    playerEntity.addEffect(new EffectInstance(Effects.MOVEMENT_SPEED, 65, 1, false, false));
                            if (playerEntity.isInWaterOrBubble())
                                playerEntity.setAirSupply(playerEntity.getMaxAirSupply());
                            if (!world.isClientSide && ConfigurationHandler.NetworkedConfig.getEnableDragonDebuffs() && !playerEntity.isCreative()) {
                                if (!playerEntity.isInWaterOrRain() && !playerEntity.isInWaterOrBubble() && !block.is(BlockTags.ICE) && !block.is(Blocks.SNOW) && !block.is(Blocks.SNOW_BLOCK)) {
                            		dragonStateHandler.getDebuffData().timeWithoutWater++;
	                        		if (dragonStateHandler.getDebuffData().timeWithoutWater == 20 * 90 + 1)
	                            			DragonSurvivalMod.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> playerEntity), new SyncCapabilityDebuff(playerEntity.getId(), dragonStateHandler.getDebuffData().timeWithoutWater, dragonStateHandler.getDebuffData().timeInDarkness));
	                                if (dragonStateHandler.getDebuffData().timeWithoutWater > 20 * 60 * 10 && !playerEntity.hasEffect(Effects.WITHER))
	                                	playerEntity.addEffect(new EffectInstance(Effects.WITHER, 80, 1, false, false));
	                                else if (dragonStateHandler.getDebuffData().timeWithoutWater > 20 * 60 * 2 && !playerEntity.hasEffect(Effects.WITHER))
	                                	playerEntity.addEffect(new EffectInstance(Effects.WITHER, 80, 0, false, false));
                                } else if (dragonStateHandler.getDebuffData().timeWithoutWater > 0) {
                                	dragonStateHandler.getDebuffData().timeWithoutWater = 0;
                            		DragonSurvivalMod.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> playerEntity), new SyncCapabilityDebuff(playerEntity.getId(), dragonStateHandler.getDebuffData().timeWithoutWater, dragonStateHandler.getDebuffData().timeInDarkness));
                            	}
                            }
                        break;
                    }
                    
                    // Dragon Particles
                    // TODO: Randomize along dragon body
                    if (world.isClientSide) {
	                    if (dragonStateHandler.getDebuffData().timeWithoutWater > 20 * 90)
	                    	world.addParticle(ParticleTypes.WHITE_ASH,
	                    			playerEntity.getX() + world.random.nextDouble() *  (world.random.nextBoolean() ? 1 : -1), 
	                    			playerEntity.getY() + 0.5F, 
	                    			playerEntity.getZ() + world.random.nextDouble() * (world.random.nextBoolean() ? 1 : -1), 
	                    			0, 0, 0);
	                    if (dragonStateHandler.getDebuffData().timeInDarkness > 0 && dragonStateHandler.getDebuffData().timeInDarkness < 20 * 10)
                        	world.addParticle(ParticleTypes.SMOKE,
                        			playerEntity.getX() + world.random.nextDouble() *  (world.random.nextBoolean() ? 1 : -1), 
                        			playerEntity.getY() + 0.5F, 
                        			playerEntity.getZ() + world.random.nextDouble() *  (world.random.nextBoolean() ? 1 : -1), 
                        			0, 0, 0);
                    }
                }
            }
        });
    }

    /**
     * Adds dragon avoidance goal
     */
    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent joinWorldEvent) {
        Entity entity = joinWorldEvent.getEntity();
        if (!(entity instanceof MonsterEntity || entity instanceof VillagerEntity || entity instanceof GolemEntity || entity instanceof HorseEntity || entity instanceof SkeletonHorseEntity) & entity instanceof CreatureEntity) {

            ((MobEntity) entity).goalSelector.addGoal(5, new AvoidEntityGoal(
                    (CreatureEntity) entity, PlayerEntity.class,
                    livingEntity -> DragonStateProvider.isDragon((PlayerEntity) livingEntity),
                    20.0F, 1.3F, 1.5F, EntityPredicates.ATTACK_ALLOWED));
        }
        if (entity instanceof HorseEntity) {
            HorseEntity horseEntity = (HorseEntity) entity;
            horseEntity.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(horseEntity, PlayerEntity.class, 0, true, false, livingEntity -> livingEntity.getCapability(DragonStateProvider.DRAGON_CAPABILITY).orElseGet(null).getLevel() != DragonLevel.ADULT));
            horseEntity.targetSelector.addGoal(4, new AvoidEntityGoal<>(horseEntity, PlayerEntity.class, livingEntity -> livingEntity.getCapability(DragonStateProvider.DRAGON_CAPABILITY).orElse(null).getLevel() == DragonLevel.ADULT, 20, 1.3, 1.5, EntityPredicates.ATTACK_ALLOWED::test));
        }
    }

    @SubscribeEvent
    public static void onCapabilityAttachment(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            event.addCapability(new ResourceLocation(DragonSurvivalMod.MODID, "playerstatehandler"), new DragonStateProvider());
            DragonSurvivalMod.LOGGER.info("Successfully attached capabilities to the " + event.getObject().getClass().getSimpleName());
        }
    }


    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity livingEntity = e.getEntityLiving();
        if (livingEntity instanceof PlayerEntity || livingEntity instanceof MagicalPredatorEntity)
            return;

        if (livingEntity instanceof AnimalEntity && livingEntity.level.getRandom().nextInt(30) == 0) {
            MagicalPredatorEntity beast = EntityTypesInit.MAGICAL_BEAST.create(livingEntity.level);
            livingEntity.level.addFreshEntity(beast);
            beast.teleportToWithTicket(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        DragonStateProvider.getCap(e.getPlayer()).ifPresent(capNew ->
                DragonStateProvider.getCap(e.getOriginal()).ifPresent(capOld -> {
                    if (capOld.isDragon()) {
                        DragonStateHandler.DragonMovementData movementData = capOld.getMovementData();
                        capNew.setMovementData(movementData.bodyYaw, movementData.headYaw, movementData.headPitch, movementData.bite);
                        capNew.setSize(capOld.getSize());
                        capNew.setType(capOld.getType());
                        capNew.setHasWings(capOld.hasWings());

                        DragonStateHandler.updateModifiers(e.getOriginal(), e.getPlayer());

                        e.getPlayer().refreshDimensions();
                    }
                }));
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent changedDimensionEvent) {
        PlayerEntity playerEntity = changedDimensionEvent.getPlayer();
        DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
            DragonSurvivalMod.CHANNEL.send(PacketDistributor.ALL.noArg(), new SynchronizeDragonCap(playerEntity.getId(), dragonStateHandler.isHiding(), dragonStateHandler.getType(), dragonStateHandler.getSize(), dragonStateHandler.hasWings()));
            DragonSurvivalMod.CHANNEL.send(PacketDistributor.ALL.noArg(), new RefreshDragons(playerEntity.getId()));
        });
    }

    @SubscribeEvent
    public static void modifyBreakSpeed(PlayerEvent.BreakSpeed breakSpeedEvent) {
        PlayerEntity playerEntity = breakSpeedEvent.getPlayer();
        DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                ItemStack mainStack = playerEntity.getMainHandItem();
                BlockState blockState = breakSpeedEvent.getState();
                Item item = mainStack.getItem();
                if (!(item instanceof ToolItem || item instanceof SwordItem || item instanceof ShearsItem)) {
                    switch (dragonStateHandler.getLevel()) {
                        case BABY:
                            breakSpeedEvent.setNewSpeed(breakSpeedEvent.getOriginalSpeed() * 2.0F);
                            break;
                        case YOUNG:
                        case ADULT:
                            switch (dragonStateHandler.getType()) {
                                case FOREST:
                                    if (blockState.isToolEffective(ToolType.AXE)) {
                                        breakSpeedEvent.setNewSpeed(breakSpeedEvent.getOriginalSpeed() * 4.0F);
                                    } else breakSpeedEvent.setNewSpeed(breakSpeedEvent.getOriginalSpeed() * 2.0F);
                                    break;
                                case CAVE:
                                    if (blockState.isToolEffective(ToolType.PICKAXE)) {
                                        breakSpeedEvent.setNewSpeed(breakSpeedEvent.getOriginalSpeed() * 4.0F);
                                    } else breakSpeedEvent.setNewSpeed(breakSpeedEvent.getOriginalSpeed() * 2.0F);
                                    break;
                                case SEA:
                                    if (blockState.isToolEffective(ToolType.SHOVEL)) {
                                        breakSpeedEvent.setNewSpeed(breakSpeedEvent.getOriginalSpeed() * 4.0F);
                                    } else breakSpeedEvent.setNewSpeed(breakSpeedEvent.getOriginalSpeed() * 2.0F);
                                    if (playerEntity.isInWaterOrBubble()) {
                                        breakSpeedEvent.setNewSpeed(breakSpeedEvent.getNewSpeed() * 1.4f);
                                    }
                                    break;
                            }
                            break;
                    }
                } else {
                    breakSpeedEvent.setNewSpeed(breakSpeedEvent.getOriginalSpeed() * 0.7f);
                }
            }
        });
    }

    @SubscribeEvent
    public static void dropBlocksMinedByPaw(PlayerEvent.HarvestCheck harvestCheck) {
        PlayerEntity playerEntity = harvestCheck.getPlayer();
        DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                ItemStack stack = playerEntity.getMainHandItem();
                Item item = stack.getItem();
                BlockState blockState = harvestCheck.getTargetBlock();
                if (!(item instanceof ToolItem || item instanceof SwordItem || item instanceof ShearsItem) && !harvestCheck.canHarvest()) {
                	harvestCheck.setCanHarvest(dragonStateHandler.canHarvestWithPaw(blockState));
            	}
            }
        });
    }

    @SubscribeEvent
    public static void disableMounts(EntityMountEvent mountEvent) {
        Entity mounting = mountEvent.getEntityMounting();
        DragonStateProvider.getCap(mounting).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                if (mountEvent.getEntityBeingMounted() instanceof AbstractHorseEntity || mountEvent.getEntityBeingMounted() instanceof PigEntity || mountEvent.getEntityBeingMounted() instanceof StriderEntity)
                    mountEvent.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onItemDestroyed(LivingEntityUseItemEvent.Finish destroyItemEvent) {
        ItemStack itemStack = destroyItemEvent.getItem();
        Item item = itemStack.getItem();
        LivingEntity livingEntity = destroyItemEvent.getEntityLiving();
        DragonStateProvider.getCap(livingEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                PlayerEntity playerEntity = (PlayerEntity) livingEntity;
                if (item.isEdible()) {
                    Food food = item.getFoodProperties();
                    assert food != null;
                    boolean bad = !food.canAlwaysEat() && item != Items.HONEY_BOTTLE;
                    switch (dragonStateHandler.getType()) {
                        case FOREST:
                            if (food == Foods.RABBIT || food == Foods.ROTTEN_FLESH || food == Foods.CHICKEN || food == Foods.BEEF || food == Foods.PORKCHOP || food == Foods.MUTTON) {
                                bad = false;
                                livingEntity.removeEffect(Effects.HUNGER);
                                if (food == Foods.CHICKEN) {
                                    playerEntity.getFoodData().eat(0, 5.8f);
                                } else if (food == Foods.PORKCHOP || food == Foods.BEEF) {
                                    playerEntity.getFoodData().eat(-1, 6.4f);
                                } else if (food == Foods.ROTTEN_FLESH) {
                                    playerEntity.getFoodData().eat(-1, 2.2f);
                                } else if (food == Foods.RABBIT) {
                                    playerEntity.getFoodData().eat(2, 11.2f);
                                }

                            }
                            break;
                        case SEA:
                            if (food == Foods.SALMON || food == Foods.TROPICAL_FISH || food == Foods.COD || food == Foods.PUFFERFISH || food == Foods.DRIED_KELP) {
                                bad = false;
                                livingEntity.removeEffect(Effects.HUNGER);
                                livingEntity.removeEffect(Effects.CONFUSION);
                                livingEntity.removeEffect(Effects.POISON);
                                if (food == Foods.TROPICAL_FISH) {
                                    playerEntity.getFoodData().eat(1, 6.8f);
                                } else if (food == Foods.SALMON) {
                                    playerEntity.getFoodData().eat(0, 6.8f);
                                } else if (food == Foods.COD) {
                                    playerEntity.getFoodData().eat(0, 6.6f);
                                } else if (food == Foods.PUFFERFISH) {
                                    playerEntity.getFoodData().eat(9, 12.8f);
                                } else {
                                    playerEntity.getFoodData().eat(1, 2.4f);
                                }
                            }
                            break;
                        case CAVE:
                            if (item == ItemsInit.chargedCoal || item == ItemsInit.charredMeat)
                                bad = false;
                            break;
                    }
                    if (bad)
                        livingEntity.addEffect(new EffectInstance(Effects.HUNGER, 20 * 60, 0));
                } else if (item instanceof PotionItem) {
                    PotionItem potionItem = (PotionItem) item;
                    if (PotionUtils.getPotion(itemStack) == Potions.WATER && dragonStateHandler.getType() == DragonType.SEA && !playerEntity.level.isClientSide) {
                    	dragonStateHandler.getDebuffData().timeWithoutWater = -3 * 60 * 20; // -3 minutes (5 minutes until wither)
                    	DragonSurvivalMod.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> playerEntity), new SyncCapabilityDebuff(playerEntity.getId(), dragonStateHandler.getDebuffData().timeWithoutWater, dragonStateHandler.getDebuffData().timeInDarkness));
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public static void consumeSpecialFood(PlayerInteractEvent.RightClickItem rightClickItem) {
        PlayerEntity playerEntity = rightClickItem.getPlayer();
        DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon() && playerEntity.getFoodData().needsFood()) {
                ItemStack itemStack = rightClickItem.getItemStack();
                Item item = itemStack.getItem();
                if (dragonStateHandler.getType() == DragonType.CAVE) {
                    if (item == Items.COAL) {
                        itemStack.shrink(1);
                        playerEntity.getFoodData().eat(1, 0.5F);
                    } else if (item == Items.CHARCOAL) {
                        itemStack.shrink(1);
                        playerEntity.getFoodData().eat(1, 1.0F);
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent jumpEvent) {
        final LivingEntity livingEntity = jumpEvent.getEntityLiving();
        DragonStateProvider.getCap(livingEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                switch (dragonStateHandler.getLevel()) {
                    case BABY:
                        livingEntity.push(0, 0.025, 0); //1+ block
                        break;
                    case YOUNG:
                        livingEntity.push(0, 0.1, 0); //1.5+ block
                        break;
                    case ADULT:
                        livingEntity.push(0, 0.15, 0); //2+ blocks
                        break;
                }
                if (livingEntity instanceof ServerPlayerEntity) {
                    if (livingEntity.getServer().isSingleplayer())
                        DragonSurvivalMod.CHANNEL.send(PacketDistributor.ALL.noArg(), new StartJump(livingEntity.getId(), 42));
                    else
                        DragonSurvivalMod.CHANNEL.send(PacketDistributor.ALL.noArg(), new StartJump(livingEntity.getId(), 21));
                }
            }
        });
    }

    @SubscribeEvent
    public static void sleepCheck(SleepingLocationCheckEvent sleepingLocationCheckEvent) {
        BlockPos sleepingLocation = sleepingLocationCheckEvent.getSleepingLocation();
        World world = sleepingLocationCheckEvent.getEntity().level;
        if (world.isNight() && world.getBlockEntity(sleepingLocation) instanceof NestEntity)
            sleepingLocationCheckEvent.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void dropDragonDust(BlockEvent.BreakEvent breakEvent) {
        if (!breakEvent.isCanceled()) {
            IWorld world = breakEvent.getWorld();
            if (world instanceof ServerWorld) {
                BlockState blockState = breakEvent.getState();
                BlockPos blockPos = breakEvent.getPos();
                PlayerEntity playerEntity = breakEvent.getPlayer();
                Block block = blockState.getBlock();
                ItemStack mainHandItem = playerEntity.getItemInHand(Hand.MAIN_HAND);
                double random;
                // Modded Ore Support
                String[] tagStringSplit = ConfigurationHandler.ORE_LOOT.oreBlocksTag.get().split(":");
                ResourceLocation ores = new ResourceLocation(tagStringSplit[0], tagStringSplit[1]);
                // Checks to make sure the ore does not drop itself or another ore from the tag (no going infinite with ores)
                ITag<Item> oresTag = ItemTags.getAllTags().getTag(ores);
                if (!oresTag.contains(block.asItem()))
                    return;
                List<ItemStack> drops = block.getDrops(blockState, new LootContext.Builder((ServerWorld) world)
                        .withParameter(LootParameters.ORIGIN, new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()))
                        .withParameter(LootParameters.TOOL, mainHandItem));
                DragonStateProvider.getCap(playerEntity).ifPresent(dragonStateHandler -> {
	                final boolean suitableOre = (playerEntity.getMainHandItem().isCorrectToolForDrops(blockState) || 
	                		(dragonStateHandler.isDragon() && dragonStateHandler.canHarvestWithPaw(blockState))) 
	                		&& drops.stream().noneMatch(item -> oresTag.contains(item.getItem()));
	                if (suitableOre && !playerEntity.isCreative()) {
	                    if (dragonStateHandler.isDragon()) {
	                        if (playerEntity.getRandom().nextDouble() < ConfigurationHandler.ORE_LOOT.dragonOreDustChance.get()) {
	                            world.addFreshEntity(new ItemEntity((World) world, blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, new ItemStack(ItemsInit.elderDragonDust)));
	                        }
	                        if (playerEntity.getRandom().nextDouble() < ConfigurationHandler.ORE_LOOT.dragonOreBoneChance.get()) {
	                            world.addFreshEntity(new ItemEntity((World) world, blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, new ItemStack(ItemsInit.elderDragonBone)));
	                        }
	                    } else {
	                        if (playerEntity.getRandom().nextDouble() < ConfigurationHandler.ORE_LOOT.humanOreDustChance.get()) {
	                            world.addFreshEntity(new ItemEntity((World) world, blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, new ItemStack(ItemsInit.elderDragonDust)));
	                        }
	                        if (playerEntity.getRandom().nextDouble() < ConfigurationHandler.ORE_LOOT.humanOreBoneChance.get()) {
	                            world.addFreshEntity(new ItemEntity((World) world, blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5, new ItemStack(ItemsInit.elderDragonBone)));
	                        }
	                    }
	                }
	            });
            }
        }
    }

    @SubscribeEvent
    public static void createAltar(PlayerInteractEvent.RightClickBlock rightClickBlock) {
        ItemStack itemStack = rightClickBlock.getItemStack();
        if (itemStack.getItem() == ItemsInit.elderDragonBone) {

            final World world = rightClickBlock.getWorld();
            final BlockPos blockPos = rightClickBlock.getPos();
            BlockState blockState = world.getBlockState(blockPos);
            final Block block = blockState.getBlock();
            boolean replace = false;
            if (block == Blocks.STONE) {
                world.setBlockAndUpdate(blockPos, BlockInit.dragon_altar3.defaultBlockState());
                replace = true;
            } else if (block == Blocks.SANDSTONE) {
                world.setBlockAndUpdate(blockPos, BlockInit.dragon_altar4.defaultBlockState());
                replace = true;
            } else if (block == Blocks.MOSSY_COBBLESTONE) {
                world.setBlockAndUpdate(blockPos, BlockInit.dragon_altar.defaultBlockState());
                replace = true;
            } else if (block == Blocks.OAK_LOG) {
                world.setBlockAndUpdate(blockPos, BlockInit.dragon_altar2.defaultBlockState());
                replace = true;
            }
            if (replace) {
                itemStack.shrink(1);
                rightClickBlock.setCanceled(true);
                world.playSound(rightClickBlock.getPlayer(), blockPos, SoundEvents.STONE_PLACE, SoundCategory.PLAYERS, 1, 1);
                rightClickBlock.setCancellationResult(ActionResultType.SUCCESS);
            }

        }
    }

    @SubscribeEvent
    public static void reduceFallDistance(LivingFallEvent livingFallEvent) {
        LivingEntity livingEntity = livingFallEvent.getEntityLiving();
        DragonStateProvider.getCap(livingEntity).ifPresent(dragonStateHandler -> {
            if (dragonStateHandler.isDragon()) {
                if (dragonStateHandler.getType() == DragonType.FOREST) {
                    livingFallEvent.setDistance(livingFallEvent.getDistance() - 5);
                }
            }
        });
    }



}
