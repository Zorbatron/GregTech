package gregtech.api.unification.material;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;
import gregtech.api.unification.material.info.MaterialFlag;
import gregtech.api.unification.material.info.MaterialFlags;
import gregtech.api.unification.material.info.MaterialIconSet;
import gregtech.api.unification.material.properties.*;
import gregtech.api.unification.stack.MaterialStack;
import gregtech.api.util.SmallDigits;
import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import stanhebben.zenscript.annotations.OperatorType;
import stanhebben.zenscript.annotations.ZenOperator;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

//@ZenClass("mods.gregtech.material.Material")
//@ZenRegister
public class Material implements Comparable<Material> {

    /**
     * Basic Info of this Material.
     *
     * @see MaterialInfo
     */
    @Nonnull
    private final MaterialInfo materialInfo;

    /**
     * Properties of this Material.
     *
     * @see MaterialProperties
     */
    @Nonnull
    private final MaterialProperties properties;

    /**
     * Generation flags of this material
     *
     * @see MaterialFlags
     */
    @Nonnull
    private final MaterialFlags flags;

    /**
     * Chemical formula of this material
     */
    private String chemicalFormula;

    // TODO Fix isotope tooltips being set toSmallDownNumbers
    private String calculateChemicalFormula() {
        if (chemicalFormula != null) return this.chemicalFormula;
        if (materialInfo.element != null) {
            return materialInfo.element.getSymbol();
        }
        if (!materialInfo.componentList.isEmpty()) {
            StringBuilder components = new StringBuilder();
            for (MaterialStack component : materialInfo.componentList)
                components.append(component.toString());
            return components.toString();
        }
        return "";
    }

    //@ZenGetter
    public String getChemicalFormula() {
        return chemicalFormula;
    }

    public Material setFormula(String formula) {
        return setFormula(formula, false);
    }

    public Material setFormula(String formula, boolean withFormatting) {
        this.chemicalFormula = withFormatting ? SmallDigits.toSmallDownNumbers(formula) : formula;
        return this;
    }

    public ImmutableList<MaterialStack> getMaterialComponents() {
        return materialInfo.componentList;
    }

    private Material(@Nonnull MaterialInfo materialInfo, @Nonnull MaterialProperties properties, @Nonnull MaterialFlags flags) {
        this.materialInfo = materialInfo;
        this.properties = properties;
        this.flags = flags;

        this.properties.setMaterial(this);
        registerMaterial(this);
    }

    // thou shall not call
    protected Material(String name) {
        materialInfo = new MaterialInfo(0, name);
        materialInfo.iconSet = MaterialIconSet.NONE;
        properties = new MaterialProperties();
        flags = new MaterialFlags();
    }

    protected void registerMaterial(Material material) {
        MaterialRegistry.register(this);
    }

    public void addFlag(MaterialFlag... flags) {
        if (MaterialRegistry.isFrozen())
            throw new IllegalStateException("Cannot add flag to material when registry is frozen!");
        this.flags.addFlags(flags).verify(this);
    }

    //@ZenMethod("hasFlagRaw")
    public boolean hasFlag(MaterialFlag flag) {
        return flags.hasFlag(flag);
    }

    public boolean hasFlags(MaterialFlag... flags) {
        return Arrays.stream(flags).allMatch(this::hasFlag);
    }

    //@ZenMethod
    //public boolean hasFlag(String flagName) {
    //    long materialFlagId = MaterialFlags.resolveFlag(flagName, getClass());
    //    return hasFlag(materialFlagId);
    //}

    protected void calculateDecompositionType() {
        if (!materialInfo.componentList.isEmpty() &&
            !hasFlag(MaterialFlags.DECOMPOSITION_BY_CENTRIFUGING) &&
            !hasFlag(MaterialFlags.DECOMPOSITION_BY_ELECTROLYZING) &&
            !hasFlag(MaterialFlags.DISABLE_DECOMPOSITION)) {
            boolean onlyMetalMaterials = true;
            for (MaterialStack materialStack : materialInfo.componentList) {
                Material material = materialStack.material;
                onlyMetalMaterials &= material.hasProperty(PropertyKey.INGOT);
            }
            //allow centrifuging of alloy materials only
            if (onlyMetalMaterials) {
                flags.addFlags(MaterialFlags.DECOMPOSITION_BY_CENTRIFUGING);
            } else {
                flags.addFlags(MaterialFlags.DECOMPOSITION_BY_ELECTROLYZING);
            }
        }
    }

    public Fluid getFluid() {
        FluidProperty prop = getProperty(PropertyKey.FLUID);
        if (prop == null)
            throw new IllegalArgumentException("Material " + materialInfo.name + " does not have a Fluid!");
        return prop.getFluid();
    }

    public FluidStack getFluid(int amount) {
        return new FluidStack(getFluid(), amount);
    }

    public int getHarvestLevel() {
        if (!hasProperty(PropertyKey.DUST))
            throw new IllegalArgumentException("Material " + materialInfo.name + " does not have a harvest level! Is probably a Fluid");
        return getProperty(PropertyKey.DUST).getHarvestLevel();
    }

    //@ZenMethod
    public void setMaterialRGB(int materialRGB) {
        materialInfo.color = materialRGB;
    }

    //@ZenGetter
    public int getMaterialRGB() {
        return materialInfo.color;
    }

    //@ZenMethod
    public void setMaterialIconSet(MaterialIconSet materialIconSet) {
        materialInfo.iconSet = materialIconSet;
    }

    public MaterialIconSet getMaterialIconSet() {
        return materialInfo.iconSet;
    }

    //@ZenGetter("radioactive")
    public boolean isRadioactive() {
        if (materialInfo.element != null)
            return materialInfo.element.halfLifeSeconds >= 0;
        for (MaterialStack material : materialInfo.componentList)
            if (material.material.isRadioactive()) return true;
        return false;
    }

    //@ZenGetter("protons")
    public long getProtons() {
        if (materialInfo.element != null)
            return materialInfo.element.getProtons();
        if (materialInfo.componentList.isEmpty())
            return Elements.get("Neutronium").getProtons();
        long totalProtons = 0;
        for (MaterialStack material : materialInfo.componentList) {
            totalProtons += material.amount * material.material.getProtons();
        }
        return totalProtons;
    }

    //@ZenGetter("neutrons")
    public long getNeutrons() {
        if (materialInfo.element != null)
            return materialInfo.element.getNeutrons();
        if (materialInfo.componentList.isEmpty())
            return Elements.get("Neutronium").getNeutrons();
        long totalNeutrons = 0;
        for (MaterialStack material : materialInfo.componentList) {
            totalNeutrons += material.amount * material.material.getNeutrons();
        }
        return totalNeutrons;
    }

    //@ZenGetter("mass")
    public long getMass() {
        if (materialInfo.element != null)
            return materialInfo.element.getMass();
        if (materialInfo.componentList.isEmpty())
            return Elements.get("Neutronium").getMass();
        long totalMass = 0;
        for (MaterialStack material : materialInfo.componentList) {
            totalMass += material.amount * material.material.getMass();
        }
        return totalMass;
    }

    //@ZenGetter("averageProtons")
    public long getAverageProtons() {
        if (materialInfo.element != null)
            return materialInfo.element.getProtons();
        if (materialInfo.componentList.isEmpty())
            return Math.max(1, Elements.get("Neutronium").getProtons());
        long totalProtons = 0, totalAmount = 0;
        for (MaterialStack material : materialInfo.componentList) {
            totalAmount += material.amount;
            totalProtons += material.amount * material.material.getAverageProtons();
        }
        return totalProtons / totalAmount;
    }

    //@ZenGetter("averageNeutrons")
    public long getAverageNeutrons() {
        if (materialInfo.element != null)
            return materialInfo.element.getNeutrons();
        if (materialInfo.componentList.isEmpty())
            return Elements.get("Neutronium").getNeutrons();
        long totalNeutrons = 0, totalAmount = 0;
        for (MaterialStack material : materialInfo.componentList) {
            totalAmount += material.amount;
            totalNeutrons += material.amount * material.material.getAverageNeutrons();
        }
        return totalNeutrons / totalAmount;
    }


    //@ZenGetter("averageMass")
    public long getAverageMass() {
        if (materialInfo.element != null)
            return materialInfo.element.getMass();
        if (materialInfo.componentList.size() <= 0)
            return Elements.get("Neutronium").getMass();
        long totalMass = 0, totalAmount = 0;
        for (MaterialStack material : materialInfo.componentList) {
            totalAmount += material.amount;
            totalMass += material.amount * material.material.getAverageMass();
        }
        return totalMass / totalAmount;
    }

    //@ZenGetter("blastTemperature")
    public int getBlastTemperature() {
        BlastProperty prop = properties.getProperty(PropertyKey.BLAST);
        return prop == null ? 0 : prop.getBlastTemperature();
    }

    public FluidStack getPlasma(int amount) {
        PlasmaProperty prop = properties.getProperty(PropertyKey.PLASMA);
        return prop == null ? null : prop.getPlasma(amount);
    }

    //@ZenGetter("camelCaseName")
    public String toCamelCaseString() {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, toString());
    }

    //@ZenGetter("unlocalizedName")
    public String getUnlocalizedName() {
        return "material." + materialInfo.name;
    }

    @SideOnly(Side.CLIENT)
    //@ZenGetter("localizedName")
    public String getLocalizedName() {
        return I18n.format(getUnlocalizedName());
    }

    @Override
    //@ZenMethod
    public int compareTo(Material material) {
        return toString().compareTo(material.toString());
    }

    @Override
    //@ZenGetter("name")
    public String toString() {
        return materialInfo.name;
    }

    public int getId() {
        return materialInfo.metaItemSubId;
    }

    @ZenOperator(OperatorType.MUL)
    public MaterialStack createMaterialStack(long amount) {
        return new MaterialStack(this, amount);
    }

    @Nonnull
    public MaterialProperties getProperties() {
        return properties;
    }

    public <T extends IMaterialProperty<T>> boolean hasProperty(PropertyKey<T> key) {
        return getProperty(key) != null;
    }

    public <T extends IMaterialProperty<T>> T getProperty(PropertyKey<T> key) {
        return properties.getProperty(key);
    }

    public <T extends IMaterialProperty<T>> void setProperty(PropertyKey<T> key, IMaterialProperty<T> property) {
        if (MaterialRegistry.isFrozen()) {
            throw new IllegalStateException("Cannot add properties to a Material when registry is frozen!");
        }
        properties.setProperty(key, property);
        properties.ensureSet(key, true);
    }

    public boolean isSolid() {
        return hasProperty(PropertyKey.INGOT) || hasProperty(PropertyKey.GEM);
    }

    public boolean hasFluid() {
        return hasProperty(PropertyKey.FLUID);
    }

    protected void verifyMaterial() {
        properties.verify();
        flags.verify(this);
    }

    protected void postVerify() {
        this.chemicalFormula = calculateChemicalFormula();
        calculateDecompositionType();
    }

    /**
     * @since GTCEu 2.0.0
     */
    public static class Builder {

        private final MaterialInfo materialInfo;
        private final MaterialProperties properties;
        private final MaterialFlags flags;

        /*
         * The temporary list of components for this Material.
         */
        private final List<MaterialStack> composition = new ArrayList<>();

        /**
         * Constructs a {@link Material}. This Builder replaces the old constructors, and
         * no longer uses a class hierarchy, instead using a {@link MaterialProperties} system.
         *
         * @param id   The MetaItemSubID for this Material. Must be unique.
         * @param name The Name of this Material. Will be formatted as
         *             "material.<name>" for the Translation Key.
         *
         * @since GTCEu 2.0.0
         */
        public Builder(int id, String name) {
            materialInfo = new MaterialInfo(id, name);
            properties = new MaterialProperties();
            flags = new MaterialFlags();
        }

        /*
         * Material Types
         */

        /**
         * Add a {@link FluidProperty} to this Material.<br>
         * Will be created as a {@link FluidType#FLUID}, without a Fluid Block.
         *
         * @throws IllegalArgumentException If a {@link FluidProperty} has already been added to this Material.
         */
        public Builder fluid() {
            properties.ensureSet(PropertyKey.FLUID);
            return this;
        }

        /**
         * Add a {@link FluidProperty} to this Material.<br>
         * Will be created without a Fluid Block.
         *
         * @param type The {@link FluidType} of this Material, either Fluid or Gas.
         *
         * @throws IllegalArgumentException If a {@link FluidProperty} has already been added to this Material.
         */
        public Builder fluid(FluidType type) {
            return fluid(type, false);
        }

        /**
         * Add a {@link FluidProperty} to this Material.
         *
         * @param type     The {@link FluidType} of this Material, either Fluid or Gas.
         * @param hasBlock If true, create a Fluid Block for this Material.
         *
         * @throws IllegalArgumentException If a {@link FluidProperty} has already been added to this Material.
         */
        public Builder fluid(FluidType type, boolean hasBlock) {
            properties.setProperty(PropertyKey.FLUID, new FluidProperty(type == FluidType.GAS, hasBlock));
            return this;
        }

        /**
         * Add a {@link PlasmaProperty} to this Material.<br>
         * Is not required to have a {@link FluidProperty}, and will not automatically apply one.
         *
         * @throws IllegalArgumentException If a {@link PlasmaProperty} has already been added to this Material.
         */
        public Builder plasma() {
            properties.ensureSet(PropertyKey.PLASMA);
            return this;
        }

        /**
         * Add a {@link DustProperty} to this Material.<br>
         * Will be created with a Harvest Level of 2 and no Burn Time (Furnace Fuel).
         *
         * @throws IllegalArgumentException If a {@link DustProperty} has already been added to this Material.
         */
        public Builder dust() {
            properties.ensureSet(PropertyKey.DUST);
            return this;
        }

        /**
         * Add a {@link DustProperty} to this Material.<br>
         * Will be created with no Burn Time (Furnace Fuel).
         *
         * @param harvestLevel The Harvest Level of this block for Mining.<br>
         *                     If this Material also has a {@link ToolProperty}, this value will
         *                     also be used to determine the tool's Mining Level.
         *
         * @throws IllegalArgumentException If a {@link DustProperty} has already been added to this Material.
         */
        public Builder dust(int harvestLevel) {
            return dust(harvestLevel, 0);
        }

        /**
         * Add a {@link DustProperty} to this Material.
         *
         * @param harvestLevel The Harvest Level of this block for Mining.<br>
         *                     If this Material also has a {@link ToolProperty}, this value will
         *                     also be used to determine the tool's Mining Level.
         * @param burnTime     The Burn Time (in ticks) of this Material as a Furnace Fuel.
         *
         * @throws IllegalArgumentException If a {@link DustProperty} has already been added to this Material.
         */
        public Builder dust(int harvestLevel, int burnTime) {
            properties.setProperty(PropertyKey.DUST, new DustProperty(harvestLevel, burnTime));
            return this;
        }

        /**
         * Add an {@link IngotProperty} to this Material.<br>
         * Will be created with a Harvest Level of 2 and no Burn Time (Furnace Fuel).<br>
         * Will automatically add a {@link DustProperty} to this Material if it does not already have one.
         *
         * @throws IllegalArgumentException If an {@link IngotProperty} has already been added to this Material.
         */
        public Builder ingot() {
            properties.ensureSet(PropertyKey.INGOT);
            return this;
        }

        /**
         * Add an {@link IngotProperty} to this Material.<br>
         * Will be created with no Burn Time (Furnace Fuel).<br>
         * Will automatically add a {@link DustProperty} to this Material if it does not already have one.
         *
         * @param harvestLevel The Harvest Level of this block for Mining.<br>
         *                     If this Material also has a {@link ToolProperty}, this value will
         *                     also be used to determine the tool's Mining level.<br>
         *                     If this Material already had a Harvest Level defined, it will be overridden.
         *
         * @throws IllegalArgumentException If an {@link IngotProperty} has already been added to this Material.
         */
        public Builder ingot(int harvestLevel) {
            return ingot(harvestLevel, 0);
        }

        /**
         * Add an {@link IngotProperty} to this Material.<br>
         * Will automatically add a {@link DustProperty} to this Material if it does not already have one.
         *
         * @param harvestLevel The Harvest Level of this block for Mining.<br>
         *                     If this Material also has a {@link ToolProperty}, this value will
         *                     also be used to determine the tool's Mining level.<br>
         *                     If this Material already had a Harvest Level defined, it will be overridden.
         * @param burnTime     The Burn Time (in ticks) of this Material as a Furnace Fuel.<br>
         *                     If this Material already had a Burn Time defined, it will be overridden.
         *
         * @throws IllegalArgumentException If an {@link IngotProperty} has already been added to this Material.
         */
        public Builder ingot(int harvestLevel, int burnTime) {
            DustProperty prop = properties.getProperty(PropertyKey.DUST);
            if (prop == null) dust(harvestLevel, burnTime);
            else {
                if (prop.getHarvestLevel() == 2) prop.setHarvestLevel(harvestLevel);
                if (prop.getBurnTime() == 0) prop.setBurnTime(burnTime);
            }
            properties.ensureSet(PropertyKey.INGOT);
            return this;
        }

        /**
         * Add a {@link GemProperty} to this Material.<br>
         * Will be created with a Harvest Level of 2 and no Burn Time (Furnace Fuel).<br>
         * Will automatically add a {@link DustProperty} to this Material if it does not already have one.
         *
         * @throws IllegalArgumentException If a {@link GemProperty} has already been added to this Material.
         */
        public Builder gem() {
            properties.ensureSet(PropertyKey.GEM);
            return this;
        }

        /**
         * Add a {@link GemProperty} to this Material.<br>
         * Will be created with no Burn Time (Furnace Fuel).<br>
         * Will automatically add a {@link DustProperty} to this Material if it does not already have one.
         *
         * @param harvestLevel The Harvest Level of this block for Mining.<br>
         *                     If this Material also has a {@link ToolProperty}, this value will
         *                     also be used to determine the tool's Mining level.<br>
         *                     If this Material already had a Harvest Level defined, it will be overridden.
         *
         * @throws IllegalArgumentException If a {@link GemProperty} has already been added to this Material.
         */
        public Builder gem(int harvestLevel) {
            return gem(harvestLevel, 0);
        }

        /**
         * Add a {@link GemProperty} to this Material.<br>
         * Will automatically add a {@link DustProperty} to this Material if it does not already have one.
         *
         * @param harvestLevel The Harvest Level of this block for Mining.<br>
         *                     If this Material also has a {@link ToolProperty}, this value will
         *                     also be used to determine the tool's Mining level.<br>
         *                     If this Material already had a Harvest Level defined, it will be overridden.
         * @param burnTime     The Burn Time (in ticks) of this Material as a Furnace Fuel.<br>
         *                     If this Material already had a Burn Time defined, it will be overridden.
         *
         */
        public Builder gem(int harvestLevel, int burnTime) {
            DustProperty prop = properties.getProperty(PropertyKey.DUST);
            if (prop == null) dust(harvestLevel, burnTime);
            else {
                if (prop.getHarvestLevel() == 2) prop.setHarvestLevel(harvestLevel);
                if (prop.getBurnTime() == 0) prop.setBurnTime(burnTime);
            }
            properties.ensureSet(PropertyKey.GEM);
            return this;
        }

        /**
         * Set the Color of this Material.<br>
         * Defaults to 0xFFFFFF if {@link MaterialInfo#componentList} is empty, otherwise
         * will be the average of all Material colors in the Component List.
         *
         * @param color The RGB-formatted Color.
         */
        public Builder color(int color) {
            this.materialInfo.color = color;
            return this;
        }

        /**
         * Set the {@link MaterialIconSet} of this Material.<br>
         * Defaults vary depending on if the Material has a:<br>
         * <ul>
         * <li> {@link GemProperty}, it will default to {@link MaterialIconSet#GEM_VERTICAL}
         * <li> {@link IngotProperty} or {@link DustProperty}, it will default to {@link MaterialIconSet#DULL}
         * <li> {@link FluidProperty}, it will default to either {@link MaterialIconSet#FLUID}
         *      or {@link MaterialIconSet#GAS}, depending on the {@link FluidType}
         * <li> {@link PlasmaProperty}, it will default to {@link MaterialIconSet#FLUID}
         * </ul>
         * Default will be determined by first-found Property in this order, unless specified.
         *
         * @param iconSet The {@link MaterialIconSet} of this Material.
         */
        public Builder iconSet(MaterialIconSet iconSet) {
            materialInfo.iconSet = iconSet;
            return this;
        }

        public Builder components(Object... components) {
            Preconditions.checkArgument(
                    components.length % 2 == 0,
                    "Material Components list malformed!"
            );

            for (int i = 0; i < components.length; i += 2) {
                composition.add(new MaterialStack(
                        (Material) components[i],
                        (Integer) components[i + 1]
                ));
            }
            return this;
        }

        /**
         * Add {@link MaterialFlags} to this Material.<br>
         * Dependent Flags (for example, {@link MaterialFlags#GENERATE_LONG_ROD} requiring
         * {@link MaterialFlags#GENERATE_ROD}) will be automatically applied.
         */
        public Builder flags(MaterialFlag... flags) {
            this.flags.addFlags(flags);
            return this;
        }

        /**
         * Add {@link MaterialFlags} to this Material.<br>
         * Dependent Flags (for example, {@link MaterialFlags#GENERATE_LONG_ROD} requiring
         * {@link MaterialFlags#GENERATE_ROD}) will be automatically applied.
         *
         * @param f1 A {@link Collection} of {@link MaterialFlag}. Provided this way for easy Flag presets to be applied.
         * @param f2 An Array of {@link MaterialFlag}. If no {@link Collection} is required, use {@link Builder#flags(MaterialFlag...)}.
         */
        public Builder flags(Collection<MaterialFlag> f1, MaterialFlag... f2) {
            this.flags.addFlags(f1.toArray(new MaterialFlag[0]));
            this.flags.addFlags(f2);
            return this;
        }

        public Builder element(Element element) {
            this.materialInfo.element = element;
            return this;
        }

        public Builder toolStats(float speed, float damage, int durability) {
            properties.setProperty(PropertyKey.TOOL, new ToolProperty(speed, damage, durability));
            return this;
        }

        public Builder blastTemp(int temp) {
            properties.setProperty(PropertyKey.BLAST, new BlastProperty(temp));
            return this;
        }

        public Builder ore() {
            properties.ensureSet(PropertyKey.ORE);
            return this;
        }

        public Builder ore(int oreMultiplier, int byproductMultiplier) {
            properties.setProperty(PropertyKey.ORE, new OreProperty(oreMultiplier, byproductMultiplier));
            return this;
        }

        public Builder fluidTemp(int temp) {
            properties.ensureSet(PropertyKey.FLUID);
            properties.getProperty(PropertyKey.FLUID).setFluidTemperature(temp);
            return this;
        }

        public Builder separatesInto(Material m) {
            properties.ensureSet(PropertyKey.ORE);
            properties.getProperty(PropertyKey.ORE).setSeparatedInto(m);
            return this;
        }

        public Builder washedIn(Material m) {
            properties.ensureSet(PropertyKey.ORE);
            properties.getProperty(PropertyKey.ORE).setWashedIn(m);
            return this;
        }

        public Builder separatedInto(Material m) {
            properties.ensureSet(PropertyKey.ORE);
            properties.getProperty(PropertyKey.ORE).setSeparatedInto(m);
            return this;
        }

        public Builder oreSmeltInto(Material m) {
            properties.ensureSet(PropertyKey.ORE);
            properties.getProperty(PropertyKey.ORE).setDirectSmeltResult(m);
            return this;
        }

        public Builder polarizesInto(Material m) {
            properties.ensureSet(PropertyKey.INGOT);
            properties.getProperty(PropertyKey.INGOT).setMagneticMaterial(m);
            return this;
        }

        public Builder arcSmeltInto(Material m) {
            properties.ensureSet(PropertyKey.INGOT);
            properties.getProperty(PropertyKey.INGOT).setArcSmeltingInto(m);
            return this;
        }

        public Builder macerateInto(Material m) {
            properties.ensureSet(PropertyKey.INGOT);
            properties.getProperty(PropertyKey.INGOT).setMacerateInto(m);
            return this;
        }

        public Builder ingotSmeltInto(Material m) {
            properties.ensureSet(PropertyKey.INGOT);
            properties.getProperty(PropertyKey.INGOT).setSmeltingInto(m);
            return this;
        }

        public Builder addOreByproducts(Material... byproducts) {
            properties.ensureSet(PropertyKey.ORE);
            properties.getProperty(PropertyKey.ORE).setOreByProducts(byproducts);
            return this;
        }

        public Builder cableProperties(long voltage, int amperage, int loss) {
            properties.setProperty(PropertyKey.WIRE, new WireProperty((int) voltage, amperage, loss));
            return this;
        }

        public Builder fluidPipeProperties(int maxTemp, int throughput, boolean gasProof) {
            properties.setProperty(PropertyKey.FLUID_PIPE, new FluidPipeProperty(maxTemp, throughput, gasProof));
            return this;
        }

        public Builder itemPipeProperties(int priority, float stacksPerSec) {
            properties.setProperty(PropertyKey.ITEM_PIPE, new ItemPipeProperty(priority, stacksPerSec));
            return this;
        }

        public Builder addDefaultEnchant(Enchantment enchant, int level) {
            if (!properties.hasProperty(PropertyKey.TOOL)) // cannot assign default here
                throw new IllegalArgumentException("Material cannot have an Enchant without Tools!");
            properties.getProperty(PropertyKey.TOOL).addEnchantmentForTools(enchant, level);
            return this;
        }

        public Material build() {
            materialInfo.componentList = ImmutableList.copyOf(composition);
            materialInfo.verifyInfo(properties);
            return new Material(materialInfo, properties, flags);
        }
    }

    /**
     * Holds the basic info for a Material, like the name, color, id, etc..
     */
    private static class MaterialInfo {
        /**
         * The unlocalized name of this Material.
         *
         * Required.
         */
        private final String name;

        /**
         * The MetaItem ID of this Material.
         *
         * Required.
         */
        private final int metaItemSubId;

        /**
         * The color of this Material.
         *
         * Default: 0xFFFFFF if no Components, otherwise it will be the average of Components.
         */
        private int color = -1;

        /**
         * The IconSet of this Material.
         *
         * Default: - GEM_VERTICAL if it has GemProperty.
         *          - DULL if has DustProperty or IngotProperty.
         *          - FLUID or GAS if only has FluidProperty or PlasmaProperty, depending on {@link FluidType}.
         */
        private MaterialIconSet iconSet;

        /**
         * The components of this Material.
         *
         * Default: none.
         */
        private ImmutableList<MaterialStack> componentList;

        /**
         * The Element of this Material, if it is a direct Element.
         *
         * Default: none.
         */
        private Element element;

        private MaterialInfo(int metaItemSubId, String name) {
            this.metaItemSubId = metaItemSubId;
            this.name = name;
        }

        private void verifyInfo(MaterialProperties p) {

            // Verify IconSet
            if (iconSet == null) {
                if (p.hasProperty(PropertyKey.GEM)) {
                    iconSet = MaterialIconSet.GEM_VERTICAL;
                } else if (p.hasProperty(PropertyKey.DUST) || p.hasProperty(PropertyKey.INGOT)) {
                    iconSet = MaterialIconSet.DULL;
                } else if (p.hasProperty(PropertyKey.FLUID)) {
                    if (p.getProperty(PropertyKey.FLUID).isGas()) {
                        iconSet = MaterialIconSet.GAS;
                    } else iconSet = MaterialIconSet.FLUID;
                } else if (p.hasProperty(PropertyKey.PLASMA))
                    iconSet = MaterialIconSet.FLUID;
                else iconSet = MaterialIconSet.DULL;
            }

            // Verify MaterialRGB
            if (color == -1) {
                if (componentList.isEmpty() || p.hasProperty(PropertyKey.FLUID))
                    color = 0xFFFFFF;
                else {
                    long colorTemp = 0;
                    int divisor = 0;
                    for (MaterialStack stack : componentList) {
                        colorTemp += stack.material.getMaterialRGB();
                        divisor++;
                    }
                    color = (int) (colorTemp / divisor);
                }
            }
        }
    }

    public enum FluidType {
        FLUID, GAS
    }
}