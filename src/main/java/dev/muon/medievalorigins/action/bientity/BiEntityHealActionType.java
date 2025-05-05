package dev.muon.medievalorigins.action.bientity;

import dev.muon.medievalorigins.action.ModBientityActionTypes;
import io.github.apace100.apoli.action.ActionConfiguration;
import io.github.apace100.apoli.action.context.BiEntityActionContext;
import io.github.apace100.apoli.action.type.BiEntityActionType;
import io.github.apace100.apoli.data.TypedDataObjectFactory;
import io.github.apace100.apoli.util.MiscUtil;
import io.github.apace100.apoli.util.modifier.Modifier;
import io.github.apace100.apoli.util.modifier.ModifierUtil;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class BiEntityHealActionType extends BiEntityActionType {
    public static final TypedDataObjectFactory<BiEntityHealActionType> DATA_FACTORY = TypedDataObjectFactory.simple(
            new SerializableData()
                    .add("base", SerializableDataTypes.FLOAT.optional(), Optional.empty())
                    .add("modifier", Modifier.DATA_TYPE, null)
                    .addFunctionedDefault("modifiers", Modifier.LIST_TYPE, data -> MiscUtil.singletonListOrNull(data.get("modifier")))
                    .validate(MiscUtil.validateAnyFieldsPresent("base", "modifier", "modifiers")),
            data -> new BiEntityHealActionType(
                    data.get("base"),
                    data.get("modifiers")
            ),
            (type, data) -> data.instance()
                    .set("base", type.baseHealing)
                    .set("modifiers", type.modifiers)
    );

    private final Optional<Float> baseHealing;
    private final List<Modifier> modifiers;

    public BiEntityHealActionType(Optional<Float> baseHealing,
                                  List<Modifier> modifiers) {
        this.baseHealing = baseHealing;
        this.modifiers = modifiers;
    }

    @Override
    public void accept(BiEntityActionContext context) {
        Entity actor = context.actor();
        Entity target = context.target();
        if (actor == null || !(target instanceof LivingEntity livingTarget)
                || actor.level().isClientSide() || target.level().isClientSide()) {
            return;
        }

        this.baseHealing
                .or(() -> getModifiedAmount(actor, livingTarget))
                .ifPresent(healing -> {
                    float totalHealing = healing;
                    livingTarget.heal(totalHealing);
                });
    }

    private Optional<Float> getModifiedAmount(Entity actor, LivingEntity target) {
        return !modifiers.isEmpty()
                ? Optional.of((float) ModifierUtil.applyModifiers(actor, modifiers, target.getMaxHealth()))
                : Optional.empty();
    }

    @Override
    public @NotNull ActionConfiguration<?> getConfig() {
        return ModBientityActionTypes.BIENTITY_HEAL;
    }
}