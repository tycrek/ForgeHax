package com.matt.forgehax.util.entity;

import static com.matt.forgehax.Helper.*;

import com.matt.forgehax.Globals;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.math.VectorUtils;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;

/** Class for dealing with the local player only */
public class LocalPlayerUtils implements Globals {
  /** Gets the players current view angles */
  public static Angle getViewAngles() {
    return PositionRotationManager.getState().getRenderClientViewAngles();
  }

  public static Angle getServerViewAngles() {
    return PositionRotationManager.getState().getRenderServerViewAngles();
  }

  public static Vec3d getVelocity() {
    return new Vec3d(getLocalPlayer().motionX, getLocalPlayer().motionY, getLocalPlayer().motionZ);
  }

  public static RayTraceResult getMouseOverBlockTrace() {
    return Optional.ofNullable(MC.objectMouseOver)
        .filter(tr -> tr.getBlockPos() != null) // no its not intelliJ
        .filter(
            tr ->
                Type.BLOCK.equals(tr.typeOfHit)
                    || !Material.AIR.equals(
                        getWorld().getBlockState(tr.getBlockPos()).getMaterial()))
        .orElse(null);
  }

  private static RayTraceResult trace(Vec3d start, Vec3d end) {
    return getWorld().rayTraceBlocks(start, end, false, true, false);
  }

  public static BlockPlacementInfo getBlockPlacementInfo(final BlockPos pos) {
    final Vec3d eyes = EntityUtils.getEyePos(getLocalPlayer());
    final Vec3d normal = getServerViewAngles().getDirectionVector().normalize();
    return Arrays.stream(EnumFacing.values())
        .map(side -> new BlockPlacementInfo(pos.offset(side), side))
        .filter(
            info -> info.getBlockState().getBlock().canCollideCheck(info.getBlockState(), false))
        .filter(
            info ->
                eyes.squareDistanceTo(info.getHitVec())
                    < getPlayerController().getBlockReachDistance()
                        * getPlayerController().getBlockReachDistance())
        .filter(info -> trace(eyes, info.getHitVec()) == null)
        .min(
            Comparator.comparingDouble(
                info -> VectorUtils.getCrosshairDistance(eyes, normal, info.getCenteredPos())))
        .orElse(null);
  }

  public static class BlockPlacementInfo {
    private final BlockPos pos;
    private final EnumFacing side;

    public BlockPlacementInfo(BlockPos pos, EnumFacing side) {
      this.pos = pos;
      this.side = side;
    }

    public BlockPos getPos() {
      return pos;
    }

    public EnumFacing getSide() {
      return side;
    }

    public EnumFacing getOppositeSide() {
      return side.getOpposite();
    }

    public Vec3d getHitVec() {
      Vec3d obb = getOBBCenter();
      return new Vec3d(getPos())
          .add(obb)
          .add(VectorUtils.multiplyBy(new Vec3d(getOppositeSide().getDirectionVec()), obb));
    }

    public Vec3d getCenteredPos() {
      return new Vec3d(getPos()).add(getOBBCenter());
    }

    private Vec3d getOBBCenter() {
      IBlockState state = getBlockState();
      AxisAlignedBB bb = state.getBoundingBox(getWorld(), getPos());
      return new Vec3d(
          bb.minX + ((bb.maxX - bb.minX) / 2.D),
          bb.minY + ((bb.maxY - bb.minY) / 2.D),
          bb.minZ + ((bb.maxZ - bb.minZ) / 2.D));
    }

    public IBlockState getBlockState() {
      return getWorld().getBlockState(getPos());
    }
  }
}
