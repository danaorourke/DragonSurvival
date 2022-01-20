package by.jackraidenph.dragonsurvival.util;

import by.jackraidenph.dragonsurvival.common.entity.DSEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import software.bernie.geckolib3.geo.render.built.GeoBone;

import javax.annotation.Nullable;
import java.util.Random;

public class Functions {

    public static int secondsToTicks(int seconds)
    {
        return seconds * 20;
    }

    public static int minutesToTicks(int minutes)
    {
        return secondsToTicks(minutes)*60;
    }

    public static int ticksToSeconds(int ticks) {
        return ticks / 20;
    }

    public static int ticksToMinutes(int ticks) {
        return ticksToSeconds(ticks) / 60;
    }

    public static float angleDifference(float angle1, float angle2){
        float phi = Math.abs(angle1 - angle2) % 360;
        float dif = phi > 180 ? 360 - phi : phi;
        int sign = (angle1 - angle2 >= 0 && angle1 - angle2 <= 180) || (angle1 - angle2 <= -180 && angle1- angle2>= -360) ? 1 : -1;
        dif *= sign;
        return dif;
    }
    
    @Nullable
    public static BlockPos findRandomSpawnPosition(Player playerEntity, int p_221298_1_, int timesToCheck, float distance) {
        int i = (p_221298_1_ == 0) ? 2 : (2 - p_221298_1_);
        BlockPos.MutableBlockPos blockpos$mutable = new BlockPos(0,0,0).mutable();

        for (int i1 = 0; i1 < timesToCheck; i1++) {
            float f = playerEntity.level.random.nextFloat() * 6.2831855F;
            double xRandom = playerEntity.getX() + Mth.floor(Mth.cos(f) * distance * i) + playerEntity.level.random.nextInt(5);
            double zRandom = playerEntity.getZ() + Mth.floor(Mth.sin(f) * distance * i) + playerEntity.level.random.nextInt(5);
            int y = playerEntity.level.getHeight(Heightmap.Types.WORLD_SURFACE, (int) xRandom, (int) zRandom);
            blockpos$mutable.set(xRandom, y, zRandom);
            ChunkPos pos = new ChunkPos((BlockPos) blockpos$mutable);
            if (playerEntity.level.hasChunksAt(blockpos$mutable.getX() - 10, blockpos$mutable.getY() - 10, blockpos$mutable.getZ() - 10, blockpos$mutable.getX() + 10, blockpos$mutable.getY() + 10, blockpos$mutable.getZ() + 10)
                && playerEntity.level.hasNearbyAlivePlayer(xRandom, y, zRandom, 10) && (NaturalSpawner.canSpawnAtBody(Type.ON_GROUND, playerEntity.level, (BlockPos) blockpos$mutable, DSEntities.HUNTER_HOUND) || (playerEntity.level.getBlockState(blockpos$mutable).is(
                    Blocks.SNOW) && playerEntity.level.getBlockState((BlockPos) blockpos$mutable).isAir()))) {
                return blockpos$mutable;
            }
        }
        return null;
    }

    public static void spawn(Mob mobEntity, BlockPos blockPos, ServerLevel serverWorld) {
        mobEntity.setPos(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
        mobEntity.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(blockPos), MobSpawnType.NATURAL, null, null);
        serverWorld.addFreshEntity(mobEntity);
    }

    public static boolean isAirOrFluid(BlockPos blockPos, Level  world, Player player, BlockHitResult blockHitResult) {
        return isAirOrFluid(blockPos, world, new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), blockHitResult));
    }
    
    public static boolean isAirOrFluid(BlockPos blockPos, Level world, BlockPlaceContext context) {
        return !world.getFluidState(blockPos).isEmpty() || world.isEmptyBlock(blockPos) || world.getBlockState(blockPos).canBeReplaced(context);
    }

    public static ListTag createRandomPattern(BannerPattern.Builder builder, int times) {
        if (times > 16)
            times = 16;
        if (times < 1)
            times = 1;
        Random random = new Random();
        for (int i = 0; i < times; i++) {
            builder = builder.addPattern(BannerPattern.values()[random.nextInt(BannerPattern.values().length)], DyeColor.values()[random.nextInt(DyeColor.values().length)]);
        }
        return builder.toListTag();
    }

    public static void copyBoneRotation(GeoBone from, GeoBone to) {
        to.setRotationX(from.getRotationX());
        to.setRotationY(from.getRotationY());
        to.setRotationZ(from.getRotationZ());
        if (!to.childBones.isEmpty()) {
            for (GeoBone childBone : to.childBones) {
                for (GeoBone bone : from.childBones) {
                    if (childBone.getName().equals(bone.getName())) {
                        copyBoneRotation(bone, childBone);
                        break;
                    }
                }
            }
        }
    }
    
}