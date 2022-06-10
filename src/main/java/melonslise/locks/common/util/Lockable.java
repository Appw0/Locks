package melonslise.locks.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;

import melonslise.locks.common.init.LocksCapabilities;
import melonslise.locks.common.init.LocksEnchantments;
import melonslise.locks.common.item.LockItem;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Lockable extends Observable implements Observer
{
	public static class State
	{
		public static final AxisAlignedBB
			VERT_Z_BB = new AxisAlignedBB(-2d/16d, -3d/16d, 0.5d/16d, 2d/16d, 3d/16d, 0.5d/16d),
			VERT_X_BB = LocksUtil.rotateY(VERT_Z_BB),
			HOR_Z_BB = LocksUtil.rotateX(VERT_Z_BB),
			HOR_X_BB = LocksUtil.rotateY(HOR_Z_BB);

		public final Vec3d pos;
		public final Orientation orient;
		public final AxisAlignedBB bb;
		public final boolean lockVisible;

		public State(Vec3d pos, Orientation orient, boolean lockVisible)
		{
			this(pos, orient, (orient.face == AttachFace.WALL ? orient.dir.getAxis() == Axis.Z ? VERT_Z_BB : VERT_X_BB : orient.dir.getAxis() == Axis.Z ? HOR_Z_BB : HOR_X_BB).offset(pos), lockVisible);
		}

		public State(Vec3d pos, Orientation orient, AxisAlignedBB bounds, boolean lockVisible)
		{
			this.pos = pos;
			this.orient = orient;
			this.bb = bounds;
			this.lockVisible = lockVisible;
		}

		@SideOnly(Side.CLIENT)
		public boolean inView(ClippingHelper clippingHelper, Vec3d origin)
		{
			return lockVisible && clippingHelper.isBoxInFrustum(this.bb.minX - origin.x, this.bb.minY - origin.y, this.bb.minZ - origin.z, this.bb.maxX - origin.x, this.bb.maxY - origin.y, this.bb.maxZ - origin.z);
		}

		@SideOnly(Side.CLIENT)
		public boolean inRange(Vec3d pos)
		{
			Minecraft mc = Minecraft.getMinecraft();
			double dist = this.pos.squareDistanceTo(pos);
			double max = mc.gameSettings.renderDistanceChunks * 8;
			return dist < max * max;
		}
	}
	
	public final Cuboid6i box;
	public final Lock lock;
	public final Orientation orient;
	public final int networkID;
	public final ItemStack stack;
	public final boolean lockVisible;

	public Map<List<IBlockState>, State> cache = new HashMap<>();

	public int prevShakeTicks, shakeTicks, maxShakeTicks;

	// Server only
	public Lockable(Cuboid6i box, Lock lock, Orientation orient, ItemStack stack, World world)
	{
		this(box, lock, orient, stack, world.getCapability(LocksCapabilities.LOCKABLE_HANDLER, null).nextId());
	}

	// Client only
	public Lockable(Cuboid6i box, Lock lock, Orientation orient, ItemStack stack, int networkID)
	{
		this.box = box;
		this.lock = lock;
		lock.addObserver(this);
		this.orient = orient;
		this.stack = stack;
		this.networkID = networkID;
		this.lockVisible = EnchantmentHelper.getEnchantmentLevel(LocksEnchantments.DISAPPEARING, this.stack) == 0;
	}

	@Override
	public void update(Observable lock, Object data)
	{
		this.setChanged();
		this.notifyObservers();
		LockItem.setOpen(this.stack, !this.lock.locked);
	}

	public void tick()
	{
		this.prevShakeTicks = this.shakeTicks;
		if(this.shakeTicks > 0)
			--this.shakeTicks;
	}

	public void shake(int ticks)
	{
		this.shakeTicks = this.prevShakeTicks = this.maxShakeTicks = ticks;
	}

	public State getLockState(World world)
	
	{
		List<IBlockState> states = new ArrayList<>(this.box.volume());
		for(BlockPos pos : this.box.getContainedBlockPositions())
		{
			if(!world.isBlockLoaded(pos))
				return null;
			states.add(world.getBlockState(pos));
		}
		State state = this.cache.get(states);
		if(state != null)
			return state;
		ArrayList<AxisAlignedBB> boxes = new ArrayList<>(4);
		for(BlockPos pos : this.box.getContainedBlockPositions())
		{
			AxisAlignedBB box = world.getBlockState(pos).getCollisionBoundingBox(world, pos);
			if(box == null || box == Block.NULL_AABB)
				continue;
			box = box.offset(pos);
			AxisAlignedBB union = box;
			Iterator<AxisAlignedBB> iterator = boxes.iterator();
			while(iterator.hasNext())
			{
				AxisAlignedBB box1 = iterator.next();
				if(LocksUtil.intersectsInclusive(union, box1))
				{
					union = union.union(box1);
					iterator.remove();
				}
			}
			boxes.add(union);
		}
		if(boxes.isEmpty())
			return null;
		EnumFacing side = this.orient.getCuboidFace();
		Vec3d center = this.box.getSideCenter(side);
		Vec3d point = center;
		double min = -1d;
		for(AxisAlignedBB box : boxes)
			for(EnumFacing side1 : EnumFacing.values())
			{
				Vec3d point1 = LocksUtil.getAABBSideCenter(box, side1).add(new Vec3d(side1.getDirectionVec()).scale(0.05d));
				double dist = center.squareDistanceTo(point1);
				if(min != -1d && dist >= min)
					continue;
				point = point1;
				min = dist;
				side = side1;
			}
		state = new State(point, Orientation.fromDirection(side, this.orient.dir), this.lockVisible);
		this.cache.put(states, state);
		return state;
	}

	@Override
	public boolean equals(Object object)
	{
		if(this == object)
			return true;
		if(!(object instanceof Lockable))
			return false;
		Lockable lockable = (Lockable) object;
		return (this.networkID == lockable.networkID) && ((this.box == null && lockable.box == null) || this.box.equals(lockable.box)) && ((this.lock == null && lockable.lock == null) || this.lock.equals(lockable.lock)) && (this.orient == lockable.orient);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.box, this.lock, this.orient, this.networkID);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Lockable{");
		sb.append("networkID: ");
		sb.append(networkID);
		sb.append(", ");
		sb.append(box);
		sb.append(", ");
		sb.append(lock);
		sb.append(", ");
		sb.append(orient);
		sb.append("}");
		return sb.toString();
	}
}