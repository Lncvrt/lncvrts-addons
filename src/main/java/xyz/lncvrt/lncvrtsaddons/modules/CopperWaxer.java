package xyz.lncvrt.lncvrtsaddons.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Oxidizable;
import net.minecraft.item.AxeItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.lncvrt.lncvrtsaddons.LncvrtsAddons;

public class CopperWaxer extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private int delayWaiting;

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("The range to attempt to hit the block at")
        .defaultValue(3)
        .range(1, 6)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay in ticks")
        .defaultValue(1)
        .range(0, 80)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Weather to rotate on right click of a copper block")
        .defaultValue(true)
        .build()
    );


    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether to render blocks that have been placed.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("The side color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232, 10))
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("The line color of the target block rendering.")
        .defaultValue(new SettingColor(197, 137, 232))
        .visible(render::get)
        .build()
    );

    public CopperWaxer() {
        super(LncvrtsAddons.CATEGORY, "copper-deoxidizer", "A module to automatically deoxidize copper!");
    }

    private static Direction getClosestFacing(BlockPos pos, Vec3d direction) {
        Direction best = Direction.NORTH;
        double bestDot = -Double.MAX_VALUE;

        for (Direction dir : Direction.values()) {
            double dot = direction.dotProduct(Vec3d.of(dir.getVector()));
            if (dot > bestDot) {
                bestDot = dot;
                best = dir;
            }
        }
        return best;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.player.getMainHandStack().getItem() instanceof AxeItem)) return;

        if (delayWaiting > 0) {
            delayWaiting--;
            return;
        }

        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -range.get(); x <= range.get(); x++) {
            for (int y = -range.get(); y <= range.get(); y++) {
                for (int z = -range.get(); z <= range.get(); z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);

                    if (!(state.getBlock() instanceof Oxidizable ox)) continue;
                    if (ox.getDegradationLevel() == Oxidizable.OxidationLevel.UNAFFECTED) continue;

                    if (!mc.player.getMainHandStack().getItem().canMine(
                        mc.player.getMainHandStack(),
                        state,
                        mc.world,
                        pos,
                        mc.player
                    )) continue;

                    Vec3d eyePos = mc.player.getEyePos();
                    Vec3d blockCenter = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                    Vec3d direction = blockCenter.subtract(eyePos).normalize();
                    BlockHitResult bhr = new BlockHitResult(blockCenter,
                        getClosestFacing(pos, direction),
                        pos,
                        false);

                    if (rotate.get()) {
                        Vec3d diff = blockCenter.subtract(eyePos);
                        double yaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;
                        double pitch = Math.toDegrees(-Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));
                        Rotations.rotate(yaw, pitch);
                    }
                    if (render.get()) RenderUtils.renderTickingBlock(pos.toImmutable(), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    delayWaiting = delay.get();
                    return;
                }
            }
        }
    }
}
