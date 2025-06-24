package su.mocks.perfectsigns.screen;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.google.gson.JsonParseException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.lwjgl.glfw.GLFW;
import su.mocks.perfectsigns.blockentity.BlockEntityWithText;
import su.mocks.perfectsigns.networking.SignEditFinishPayload;
import su.mocks.perfectsigns.text.TextContext;
import su.mocks.perfectsigns.util.HorizontalAlign;
import su.mocks.perfectsigns.util.PerfectSignsUtils;
import su.mocks.perfectsigns.util.TextBridge;
import su.mocks.perfectsigns.util.VerticalAlign;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 编辑告示牌时的屏幕。
 */
@Environment(EnvType.CLIENT)
public abstract class AbstractSignBlockEditScreen<T extends BlockEntityWithText> extends Screen {
  
  // 调试开关：设为1启用调试输出，设为0禁用调试输出
  private static final int DEBUG = 0;
  // 由于需要多次使用，故作为字段存储。
  private static final MutableText BUTTON_CLEAR_MESSAGE =
      TextBridge.translatable("message.perfectsigns.clear");
  private static final MutableText BUTTON_CLEAR_CONFIRM_MESSAGE =
      TextBridge.translatable("message.perfectsigns.clear.confirm");
  private static final MutableText BUTTON_CLEAR_CONFIRM_DESCRIPTION_MESSAGE =
      TextBridge.translatable("message.perfectsigns.clear.confirm.description");
  private static final MutableText BUTTON_CLEAR_DESCRIPTION_MESSAGE =
      TextBridge.translatable("message.perfectsigns.clear.description");
  
  public final BlockPos blockPos;
  public final List<TextContext> textContextsEditing;
  public boolean hidden = false;
  private static final ButtonWidget.PressAction EMPTY_PRESS_ACTION = button -> {};

  protected final RegistryWrapper.WrapperLookup registryLookup;
  public final T entity;

  /**
   * 是否发生了改变。如果改变了，则提交时发送完整内容，否则发送空 NBT 表示未做更改。
   */
  public boolean changed = false;

  public TextFieldListWidget textFieldListWidget;

  /**
   * 仅用于在 {@link #init()} 中保持子元素，其他时候可能为 {@code null}。一般建议使用 {@code textFieldListWidget.children()}。
   */
  protected List<TextFieldListWidget.Entry> textFieldListChildren;

  /**
   * 正在被选中的 TextWidget。会在 {@link #setFocused(Element)} 时更改。
   */
  @NotNull
  protected @UnmodifiableView List<@NotNull TextFieldWidget> selectedTextFields = List.of();

  /**
   * 正在被选中的 TextContent。会在 {@link TextFieldListWidget#setFocused(Element)} 时更改。
   */
  @NotNull
  protected @UnmodifiableView List<@NotNull TextContext> selectedTextContexts = List.of();

  // 按钮变量，在init方法中初始化
  public ButtonWidget addTextButton;
  public ButtonWidget removeTextButton;
  public ButtonWidget moveUpButton;
  public ButtonWidget moveDownButton;
  public ButtonWidget clearButton;
  public ButtonWidget placeHolder;
  public BooleanButtonWidget boldButton;
  public BooleanButtonWidget italicButton;
  public BooleanButtonWidget underlineButton;
  public BooleanButtonWidget strikethroughButton;
  public BooleanButtonWidget obfuscatedButton;
  public BooleanButtonWidget shadeButton;
  public FloatButtonWidget sizeButton;
  public FloatButtonWidget offsetXButton;
  public FloatButtonWidget offsetYButton;
  public FloatButtonWidget offsetZButton;
  public FloatButtonWidget colorButton;
  public FloatButtonWidget outlineColorButton;
  public FloatButtonWidget rotationXButton;
  public FloatButtonWidget rotationYButton;
  public FloatButtonWidget rotationZButton;
  public FloatButtonWidget scaleXButton;
  public FloatButtonWidget scaleYButton;
  public FloatButtonWidget horizontalAlignButton;
  public FloatButtonWidget verticalAlignButton;
  public BooleanButtonWidget seeThroughButton;
  public BooleanButtonWidget absoluteButton;
  
  // 自定义值设置相关
  private boolean isSelectingButtonToSetCustom = false;
  private boolean isAcceptingCustomValue = false;
  public ButtonWidget setCustomValueButton;
  public TextFieldWidget customValueTextField;
  private Float customValueBeforeChange;
  private FloatButtonWidget customValueFor;
  private String customValueForType; // 用于标识按钮类型
  public ButtonWidget customValueConfirmButton;
  public ButtonWidget customValueCancelButton;
  
  public ButtonWidget finishButton;
  public ButtonWidget cancelButton;
  public ButtonWidget rearrangeButton;
  public BooleanButtonWidget hideButton;
  public ButtonWidget showButton; // 在隐藏界面时显示的"显示界面"按钮
  
  // 预设模板按钮
  public ButtonWidget doubleLineTemplateButton;
  public ButtonWidget leftArrowTemplateButton;
  public ButtonWidget rightArrowTemplateButton;

  public AbstractSignBlockEditScreen(RegistryWrapper.WrapperLookup registryLookup, T entity, BlockPos blockPos, List<TextContext> textContextsEditing) {
    super(TextBridge.translatable("screen.perfectsigns.edit_wall_sign"));
    this.registryLookup = registryLookup;
    this.entity = entity;
    this.blockPos = blockPos;
    this.textContextsEditing = textContextsEditing;
  }

  @Override
  public List<? extends Element> children() {
    return super.children();
  }

  public void rearrange() {
    PerfectSignsUtils.rearrange(textContextsEditing);
    changed = true;
  }

  public void cancelEditing() {
    changed = false;
    if (this.client != null) {
      this.client.setScreen(null);
    }
  }

  @Override
  protected void init() {
    super.init();
    textFieldListWidget = new TextFieldListWidget(this, client, width, height - 90, 25, 16);
    selectedTextFields = Lists.transform(textFieldListWidget.selectedEntries, input -> input.textFieldWidget);
    selectedTextContexts = Lists.transform(textFieldListWidget.selectedEntries, input -> input.textContext);
    textFieldListWidget.children().clear();

    // 添加组件
    this.addDrawable(textFieldListWidget);

    // 创建所有按钮
    createButtons();
    
    // 添加按钮到界面
    if (!isAcceptingCustomValue && !isSelectingButtonToSetCustom) {
      Arrays.stream(getToolboxTop()).forEach(this::addDrawableChild);
      Arrays.stream(getToolboxTemplates()).forEach(this::addDrawableChild);
    }
    
    if (!isAcceptingCustomValue && !isSelectingButtonToSetCustom) {
      this.addSelectableChild(textFieldListWidget);
    }
    initTextHolders();
    
    final Stream<ClickableWidget> stream = Streams.concat(Arrays.stream(getToolbox1()), Arrays.stream(getToolbox2()), Arrays.stream(getToolbox3()));
    if (isAcceptingCustomValue) {
      stream.forEach(clickableWidget -> clickableWidget.active = false);
      this.addDrawableChild(customValueTextField);
      this.addDrawableChild(customValueConfirmButton);
      this.addDrawableChild(customValueCancelButton);
    } else if (isSelectingButtonToSetCustom) {
      stream.peek(clickableWidget -> clickableWidget.active = (clickableWidget instanceof FloatButtonWidget && clickableWidget != horizontalAlignButton && clickableWidget != verticalAlignButton) || clickableWidget == setCustomValueButton).forEach(this::addDrawableChild);
    } else {
      stream.peek(clickableWidget -> clickableWidget.active = true).forEach(this::addDrawableChild);
    }

    // 添加文本框
    if (textFieldListChildren != null) {
      textFieldListWidget.children().addAll(textFieldListChildren);
    } else {
      for (int i = 0, textContextsEditingSize = textContextsEditing.size();
           i < textContextsEditingSize;
           i++) {
        TextContext textContext = textContextsEditing.get(i);
        addTextField(i, textContext, true, false);
      }
    }
    updateTextHoldersVisibility();
    textFieldListChildren = textFieldListWidget.children();

    arrangeToolboxButtons();
    
    // 设置showButton的位置与hideButton一致
    showButton.setPosition(hideButton.getX(), hideButton.getY());

    placeHolder.setPosition(width / 2 - 100, 50);

    if (!textFieldListWidget.children().isEmpty()) {
      setFocused(textFieldListWidget);
    }
  }

  private void createButtons() {
    // 上方第一行按钮
    addTextButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.add_text"), button1 -> {
      if (textFieldListWidget.selectedEntries.isEmpty()) {
        addTextField(textFieldListWidget.children().size(), false);
      } else {
        final List<TextFieldListWidget.Entry> selectedCopy = Lists.reverse(textFieldListWidget.children()).stream().filter(textFieldListWidget.selectedEntries::contains).toList();
        for (TextFieldListWidget.Entry selectedEntry : textFieldListWidget.selectedEntries) {
          selectedEntry.setFocused(false);
        }
        textFieldListWidget.selectedEntries.clear();
        for (TextFieldListWidget.Entry entry : selectedCopy) {
          final int i = textFieldListWidget.children().indexOf(entry);
          if (i < 0) {
            continue;
          }
          addTextField(i + 1, true);
        }
      }
    }).position(width / 2 - 120 - 100, 5).size(80, 20).tooltip(Tooltip.of(TextBridge.translatable("message.perfectsigns.add_text.description").append(ScreenTexts.LINE_BREAK).append(PerfectSignsUtils.describeShortcut(TextBridge.literal("Ctrl + Shift + ").append(TextBridge.translatable("message.perfectsigns.keyboard_shortcut.equal")))))).build();

    removeTextButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.remove_text"), button -> {
      if (selectedTextFields.isEmpty()) {
        return;
      }

      final List<TextFieldListWidget.Entry> selectedCopy = Lists.reverse(textFieldListWidget.children()).stream().filter(textFieldListWidget.selectedEntries::contains).toList();
      for (TextFieldListWidget.Entry selectedEntry : textFieldListWidget.selectedEntries) {
        selectedEntry.setFocused(false);
      }
      textFieldListWidget.selectedEntries.clear();

      for (TextFieldListWidget.Entry entry : selectedCopy) {
        final int index = textFieldListWidget.children().indexOf(entry);
        if (index >= 0) {
          removeTextField(index, true);
        }
      }
    }).dimensions(width / 2 + 120 - 100, 5, 80, 20).tooltip(Tooltip.of(TextBridge.translatable("message.perfectsigns.remove_text.description").append(ScreenTexts.LINE_BREAK).append(PerfectSignsUtils.describeShortcut(TextBridge.literal("Ctrl + Shift + ").append(TextBridge.translatable("message.perfectsigns.keyboard_shortcut.minus")))))).build();

    moveUpButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.moveUp"), button -> {
      if (selectedTextFields.isEmpty()) {
        return;
      }

      final List<TextFieldListWidget.Entry> selectedCopy = textFieldListWidget.children().stream().filter(textFieldListWidget.selectedEntries::contains).toList();
      textFieldListWidget.selectedEntries.clear();
      for (TextFieldListWidget.Entry entry : selectedCopy) {
        final int i = textFieldListWidget.children().indexOf(entry);
        if (i < 0) {
          continue;
        } else if (i == 0) {
          textFieldListWidget.selectedEntries.addAll(selectedCopy);
          break;
        }
        removeTextField(i, false);
        addTextField(i - 1, entry.textContext, false, true);
      }
    }).dimensions(this.width - 20, 5, 80, 20).tooltip(Tooltip.of(TextBridge.translatable("message.perfectsigns.moveUp.description").append(ScreenTexts.LINE_BREAK).append(PerfectSignsUtils.describeShortcut(TextBridge.literal("Ctrl + Shift + ").append(TextBridge.translatable("key.keyboard.up")))))).build();

    moveDownButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.moveDown"), button -> {
      if (selectedTextFields.isEmpty()) {
        return;
      }

      final List<TextFieldListWidget.Entry> selectedCopy = Lists.reverse(textFieldListWidget.children()).stream().filter(textFieldListWidget.selectedEntries::contains).toList();
      textFieldListWidget.selectedEntries.clear();
      for (TextFieldListWidget.Entry entry : selectedCopy) {
        final int i = textFieldListWidget.children().indexOf(entry);
        if (i < 0) {
          continue;
        } else if (i == textFieldListWidget.children().size() - 1) {
          textFieldListWidget.selectedEntries.addAll(selectedCopy);
          break;
        }
        removeTextField(i, false);
        addTextField(i + 1, entry.textContext, false, true);
      }
    }).dimensions(this.width - 20 + 85, 5, 80, 20).tooltip(Tooltip.of(TextBridge.translatable("message.perfectsigns.moveDown.description").append(ScreenTexts.LINE_BREAK).append(PerfectSignsUtils.describeShortcut(TextBridge.literal("Ctrl + Shift + ").append(TextBridge.translatable("key.keyboard.down")))))).build();

    clearButton = new ButtonWidget.Builder(BUTTON_CLEAR_MESSAGE, button -> {
      if (button.getMessage().equals(BUTTON_CLEAR_MESSAGE)) {
        button.setMessage(BUTTON_CLEAR_CONFIRM_MESSAGE);
        button.setTooltip(Tooltip.of(BUTTON_CLEAR_CONFIRM_DESCRIPTION_MESSAGE));
      } else {
        textContextsEditing.clear();
        textFieldListWidget.children().clear();
        updateTextHoldersVisibility();
        changed = true;
        button.setMessage(BUTTON_CLEAR_MESSAGE);
        button.setTooltip(Tooltip.of(BUTTON_CLEAR_DESCRIPTION_MESSAGE));
      }
    }).position(width / 2 + 200 - 100, 5).size(80, 20).tooltip(Tooltip.of(BUTTON_CLEAR_DESCRIPTION_MESSAGE)).build();

    placeHolder = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.add_first_text"), button -> {
      addTextField(0, false);
    }).position(0, 0).size(200, 20).build();

    // 格式化按钮
    boldButton = new BooleanButtonWidget(this.width / 2 - 200, this.height - 50, 20, 20, TextBridge.translatable("message.perfectsigns.bold"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.bold, b -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.bold = b;
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);

    italicButton = new BooleanButtonWidget(this.width / 2 - 180, this.height - 50, 20, 20, TextBridge.translatable("message.perfectsigns.italic"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.italic, b -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.italic = b;
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);

    underlineButton = new BooleanButtonWidget(this.width / 2 - 160, this.height - 50, 20, 20, TextBridge.translatable("message.perfectsigns.underline"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.underline, b -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.underline = b;
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);

    strikethroughButton = new BooleanButtonWidget(this.width / 2 - 140, this.height - 50, 20, 20, TextBridge.translatable("message.perfectsigns.strikethrough"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.strikethrough, b -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.strikethrough = b;
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);

    obfuscatedButton = new BooleanButtonWidget(this.width / 2 - 120, this.height - 50, 20, 20, TextBridge.translatable("message.perfectsigns.obfuscated"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.obfuscated, b -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.obfuscated = b;
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);

    shadeButton = new BooleanButtonWidget(this.width / 2 - 100, this.height - 50, 35, 20, TextBridge.translatable("message.perfectsigns.shade"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.shadow, b -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.shadow = b;
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);

    // 数值按钮
    sizeButton = new FloatButtonWidget(this.width / 2 - 60, this.height - 50, 35, 20, TextBridge.translatable("message.perfectsigns.size"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.size, (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.size = valueFunction.apply(original);
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);
    sizeButton.min = 1;
    sizeButton.max = 128;
    sizeButton.defaultValue = 8;

    offsetXButton = new FloatButtonWidget(this.width / 2 - 10, this.height - 50, 40, 20, TextBridge.translatable("message.perfectsigns.offsetX"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.offsetX, (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.offsetX = valueFunction.apply(original);
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);
    offsetXButton.step = 0.5f;

    offsetYButton = new FloatButtonWidget(this.width / 2 + 40, this.height - 50, 40, 20, TextBridge.translatable("message.perfectsigns.offsetY"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.offsetY, (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.offsetY = valueFunction.apply(original);
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);
    offsetYButton.step = 0.5f;

    offsetZButton = new FloatButtonWidget(this.width / 2 + 90, this.height - 50, 40, 20, TextBridge.translatable("message.perfectsigns.offsetZ"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.offsetZ, (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.offsetZ = valueFunction.apply(original);
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);
    offsetZButton.step = 0.1f;

    colorButton = new FloatButtonWidget(0, 0, 50, 20, TextBridge.translatable("message.perfectsigns.color"), button -> {
      if (selectedTextContexts.isEmpty()) {
        return null;
      }
      final DyeColor dyeColor = PerfectSignsUtils.colorBySignColor(textFieldListWidget.getFocused().textContext.color);
      if (dyeColor == null) {
        return -2f;
      } else {
        return (float) dyeColor.getId();
      }
    }, (valueFunction, original) -> {
      changed = true;
      final int color = DyeColor.byId((int) valueFunction.get(original.floatValue())).getSignColor();
      for (TextContext textContext : selectedTextContexts) {
        textContext.color = color;
      }
    }, button -> {
      // 颜色按钮点击时，如果正在选择自定义值按钮，则开始接受自定义值
      if (isSelectingButtonToSetCustom) {
        customValueStartAccepting(colorButton);
      }
    }).nameValueAs(colorId -> {
      if (colorId == -2 && !selectedTextContexts.isEmpty()) {
        return PerfectSignsUtils.describeColor(textFieldListWidget.getFocused().textContext.color);
      } else {
        final DyeColor dyeColor = DyeColor.byId((int) colorId);
        return PerfectSignsUtils.describeColor(dyeColor.getSignColor(), TextBridge.translatable("color.minecraft." + dyeColor.asString()));
      }
    }).setRenderedNameSupplier((value, valueText) -> valueText);

    outlineColorButton = new FloatButtonWidget(0, 0, 70, 20, TextBridge.translatable("message.perfectsigns.outline_color"), button -> {
      if (selectedTextContexts.isEmpty()) {
        return null;
      }
      if (textFieldListWidget.getFocused().textContext.outlineColor == -1) {
        return -1f; // 自动描边
      } else if (textFieldListWidget.getFocused().textContext.outlineColor == -2) {
        return -2f; // 无描边
      }
      final DyeColor color = PerfectSignsUtils.colorBySignColor(textFieldListWidget.getFocused().textContext.outlineColor);
      if (color != null) {
        return (float) color.getId();
      } else {
        return -3f; // 自定义颜色
      }
    }, (valueFunction, original) -> {
      changed = true;
      final int colorId = (int) valueFunction.get(original.floatValue());
      final int outlineColor;
      if (colorId == -1) {
        outlineColor = -1; // 自动描边
      } else if (colorId == -2) {
        outlineColor = -2; // 无描边
      } else {
        outlineColor = DyeColor.byId(colorId).getSignColor();
      }
      for (TextContext textContext : selectedTextContexts) {
        textContext.outlineColor = outlineColor;
      }
    }, button -> {
      // 描边颜色按钮点击时，如果正在选择自定义值按钮，则开始接受自定义值
      if (isSelectingButtonToSetCustom) {
        customValueStartAccepting(outlineColorButton);
      }
    }).nameValueAs(colorId -> {
      if (colorId == -1) {
        return TextBridge.translatable("message.perfectsigns.outline_color.auto");
      } else if (colorId == -2) {
        return TextBridge.translatable("message.perfectsigns.outline_color.none");
      } else if (colorId == -3 && !selectedTextContexts.isEmpty()) {
        return PerfectSignsUtils.describeColor(textFieldListWidget.getFocused().textContext.outlineColor);
      } else {
        final DyeColor color = DyeColor.byId((int) colorId);
        if (color == null)
          return TextBridge.translatable("message.perfectsigns.outline_color.none");
        return PerfectSignsUtils.describeColor(color.getSignColor(), TextBridge.translatable("color.minecraft." + color.asString()));
      }
    }).setRenderedNameSupplier((value, valueText) -> {
      if (value == null) {
        return null;
      } else if (value == -1) {
        return TextBridge.translatable("message.perfectsigns.outline_color.composed.auto");
      } else if (value == -2) {
        return TextBridge.translatable("message.perfectsigns.outline_color.composed.none");
      } else if (!selectedTextContexts.isEmpty()) {
        return TextBridge.translatable("message.perfectsigns.outline_color.composed", PerfectSignsUtils.describeColor(textFieldListWidget.getFocused().textContext.outlineColor));
      } else {
        return null;
      }
    });

    // 旋转按钮
    rotationXButton = new FloatButtonWidget(this.width / 2 + 40, this.height - 50, 40, 20, TextBridge.translatable("message.perfectsigns.rotationX"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.rotationX, (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.rotationX = valueFunction.apply(original);
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);
    rotationXButton.min = -180f;
    rotationXButton.max = 180f;
    rotationXButton.step = 15f;

    rotationYButton = new FloatButtonWidget(this.width / 2 + 40, this.height - 50, 40, 20, TextBridge.translatable("message.perfectsigns.rotationY"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.rotationY, (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.rotationY = valueFunction.apply(original);
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);
    rotationYButton.min = -180f;
    rotationYButton.max = 180f;
    rotationYButton.step = 15f;

    rotationZButton = new FloatButtonWidget(this.width / 2 + 40, this.height - 50, 40, 20, TextBridge.translatable("message.perfectsigns.rotationZ"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.rotationZ, (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.rotationZ = valueFunction.apply(original);
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);
    rotationZButton.min = -180f;
    rotationZButton.max = 180f;
    rotationZButton.step = 15f;

    // 缩放按钮
    scaleXButton = new FloatButtonWidget(this.width / 2 + 90, this.height - 50, 40, 20, TextBridge.translatable("message.perfectsigns.scaleX"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.scaleX, (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.scaleX = valueFunction.apply(original);
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);
    scaleXButton.min = 0.1f;
    scaleXButton.max = 10f;
    scaleXButton.step = 0.1f;
    scaleXButton.defaultValue = 1f;

    scaleYButton = new FloatButtonWidget(this.width / 2 + 140, this.height - 50, 40, 20, TextBridge.translatable("message.perfectsigns.scaleY"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.scaleY, (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.scaleY = valueFunction.apply(original);
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);
    scaleYButton.min = 0.1f;
    scaleYButton.max = 10f;
    scaleYButton.step = 0.1f;
    scaleYButton.defaultValue = 1f;

    // 对齐按钮
    horizontalAlignButton = new FloatButtonWidget(0, 0, 50, 20, TextBridge.translatable("message.perfectsigns.horizontal_align"), b -> selectedTextContexts.isEmpty() ? null : (float) textFieldListWidget.getFocused().textContext.horizontalAlign.ordinal(), (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        int ordinal = MathHelper.clamp(valueFunction.apply(original).intValue(), 0, HorizontalAlign.values().length - 1);
        textContext.horizontalAlign = HorizontalAlign.values()[ordinal];
      }
      changed = true;
    }, EMPTY_PRESS_ACTION).setRenderedNameSupplier((value, valueText) -> {
      if (value == null) return null;
      int ordinal = MathHelper.clamp((int) (float) value, 0, HorizontalAlign.values().length - 1);
      HorizontalAlign align = HorizontalAlign.values()[ordinal];
      return TextBridge.translatable("horizontal_align.perfectsigns." + align.asString());
    }).nameValueAs(value -> {
      int ordinal = MathHelper.clamp((int) (float) value, 0, HorizontalAlign.values().length - 1);
      HorizontalAlign align = HorizontalAlign.values()[ordinal];
      return TextBridge.translatable("horizontal_align.perfectsigns." + align.asString());
    });

    verticalAlignButton = new FloatButtonWidget(0, 0, 50, 20, TextBridge.translatable("message.perfectsigns.vertical_align"), b -> selectedTextContexts.isEmpty() ? null : (float) textFieldListWidget.getFocused().textContext.verticalAlign.ordinal(), (valueFunction, original) -> {
      for (TextContext textContext : selectedTextContexts) {
        int ordinal = MathHelper.clamp(valueFunction.apply(original).intValue(), 0, VerticalAlign.values().length - 1);
        textContext.verticalAlign = VerticalAlign.values()[ordinal];
      }
      changed = true;
    }, EMPTY_PRESS_ACTION).setRenderedNameSupplier((value, valueText) -> {
      if (value == null) return null;
      int ordinal = MathHelper.clamp((int) (float) value, 0, VerticalAlign.values().length - 1);
      VerticalAlign align = VerticalAlign.values()[ordinal];
      return TextBridge.translatable("vertical_align.perfectsigns." + align.asString());
    }).nameValueAs(value -> {
      int ordinal = MathHelper.clamp((int) (float) value, 0, VerticalAlign.values().length - 1);
      VerticalAlign align = VerticalAlign.values()[ordinal];
      return TextBridge.translatable("vertical_align.perfectsigns." + align.asString());
    });

    // 其他布尔按钮
    seeThroughButton = new BooleanButtonWidget(0, 0, 60, 20, TextBridge.translatable("message.perfectsigns.see_through"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.seeThrough, b -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.seeThrough = b;
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);

    absoluteButton = new BooleanButtonWidget(0, 0, 50, 20, TextBridge.translatable("message.perfectsigns.absolute"), button -> selectedTextContexts.isEmpty() ? null : textFieldListWidget.getFocused().textContext.absolute, b -> {
      for (TextContext textContext : selectedTextContexts) {
        textContext.absolute = b;
      }
      changed = true;
    }, EMPTY_PRESS_ACTION);

    // 自定义值设置按钮
    setCustomValueButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.set_custom_value"), button -> {
      isSelectingButtonToSetCustom = !isSelectingButtonToSetCustom;
      clearAndInit();
    }).dimensions(this.width / 2, this.height - 50, 80, 20).tooltip(Tooltip.of(TextBridge.translatable("message.perfectsigns.set_custom_value.description"))).build();

    // 自定义值输入框
    customValueTextField = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 5, height - 40, width - 112, 20, TextBridge.translatable("message.perfectsigns.custom_value"));

    // 自定义值确认和取消按钮
    customValueConfirmButton = ButtonWidget.builder(ScreenTexts.OK, button -> {
      if (DEBUG == 1) System.out.println("Custom value confirm button clicked");
      // 在确认时重新尝试解析和应用值
      if (customValueFor != null) {
        final String text = customValueTextField.getText();
        if (DEBUG == 1) {
          System.out.println("Confirming custom value: " + text);
          System.out.println("customValueFor: " + customValueFor);
          System.out.println("colorButton: " + colorButton);
          System.out.println("customValueForType: " + customValueForType);
        }
        
        if ("color".equals(customValueForType)) {
          final Integer parse = PerfectSignsUtils.parseColor(text).result().orElse(null);
          if (DEBUG == 1) System.out.println("Final color parse result: " + parse);
          if (parse != null) {
            for (TextContext textContext : selectedTextContexts) {
              textContext.color = parse;
            }
            changed = true;
            if (DEBUG == 1) System.out.println("Color applied successfully: " + parse);
          } else {
            if (DEBUG == 1) System.out.println("Color parsing failed, keeping original");
          }
        } else if ("outlineColor".equals(customValueForType)) {
          if (text.equalsIgnoreCase("auto")) {
            for (TextContext textContext : selectedTextContexts) {
              textContext.outlineColor = -1;
            }
            changed = true;
          } else if (text.equalsIgnoreCase("none")) {
            for (TextContext textContext : selectedTextContexts) {
              textContext.outlineColor = -2;
            }
            changed = true;
          } else {
            final Integer parse = PerfectSignsUtils.parseColor(text).result().orElse(null);
            if (parse != null) {
              for (TextContext textContext : selectedTextContexts) {
                textContext.outlineColor = parse;
              }
              changed = true;
            }
          }
        } else {
          // 处理数值按钮
          try {
            final float value = Float.parseFloat(text);
            customValueFor.setAllSameValue(value);
            changed = true;
          } catch (NumberFormatException e) {
            if (DEBUG == 1) System.out.println("Number parsing failed: " + e.getMessage());
          }
        }
      }
      customValueStopAccepting();
    }).dimensions(width - 105, height - 40, 50, 20).build();
    
    customValueCancelButton = ButtonWidget.builder(ScreenTexts.CANCEL, button -> {
      if (customValueFor != null) {
        if ("color".equals(customValueForType)) {
          for (TextContext textContext : selectedTextContexts) {
            textContext.color = customValueBeforeChange.intValue();
          }
        } else if ("outlineColor".equals(customValueForType)) {
          for (TextContext textContext : selectedTextContexts) {
            if (customValueBeforeChange == -0.25f) {
              textContext.outlineColor = -2;
            } else if (customValueBeforeChange == -0.125f) {
              textContext.outlineColor = -1;
            } else {
              textContext.outlineColor = customValueBeforeChange.intValue();
            }
          }
        } else {
          customValueFor.setAllSameValue(customValueBeforeChange == null ? customValueFor.defaultValue : customValueBeforeChange);
        }
      }
      customValueStopAccepting();
    }).dimensions(width - 55, height - 40, 50, 20).build();

    // 底部按钮
    finishButton = new ButtonWidget.Builder(ScreenTexts.DONE, buttonWidget -> this.finishEditing()).dimensions(this.width / 2 - 100, this.height - 30, 170, 20).tooltip(Tooltip.of(TextBridge.translatable("message.perfectsigns.finish.description"))).build();

    cancelButton = new ButtonWidget.Builder(ScreenTexts.CANCEL, button -> this.cancelEditing()).dimensions(this.width / 2, height - 30, 40, 20).tooltip(Tooltip.of(TextBridge.translatable("message.perfectsigns.cancel.description"))).build();

    rearrangeButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.rearrange"), button -> rearrange()).dimensions(this.width / 2, this.height - 50, 40, 20).tooltip(Tooltip.of(TextBridge.translatable("message.perfectsigns.rearrange.tooltip"))).build();

    hideButton = new BooleanButtonWidget(0, height - 25, 40, 20, TextBridge.translatable("message.perfectsigns.hide_gui"), booleanButtonWidget -> hidden, value -> {
      hidden = value;
    }, EMPTY_PRESS_ACTION);
    
    showButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.show_gui"), button -> {
      hidden = false;
    }).dimensions(0, height - 25, 40, 20).build();
    
    // 预设模板按钮
    doubleLineTemplateButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.apply_double_line_template"), button -> {
      applyDoubleLineTemplate();
    }).dimensions(0, 0, 80, 20).build();
    
    leftArrowTemplateButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.apply_left_arrow_template"), button -> {
      applyLeftArrowTemplate();
    }).dimensions(0, 0, 80, 20).build();
    
    rightArrowTemplateButton = new ButtonWidget.Builder(TextBridge.translatable("message.perfectsigns.apply_right_arrow_template"), button -> {
      applyRightArrowTemplate();
    }).dimensions(0, 0, 80, 20).build();
  }

  public ClickableWidget[] getToolboxTop() {
    return new ClickableWidget[]{addTextButton, removeTextButton, moveUpButton, moveDownButton, clearButton};
  }

  public ClickableWidget[] getToolboxTemplates() {
    return new ClickableWidget[]{doubleLineTemplateButton, leftArrowTemplateButton, rightArrowTemplateButton};
  }

  public ClickableWidget[] getToolbox1() {
    return new ClickableWidget[]{boldButton, italicButton, underlineButton, strikethroughButton, obfuscatedButton, shadeButton, sizeButton, offsetXButton, offsetYButton, offsetZButton, colorButton, outlineColorButton};
  }

  public ClickableWidget[] getToolbox2() {
    return new ClickableWidget[]{rotationXButton, rotationYButton, rotationZButton, scaleXButton, scaleYButton, horizontalAlignButton, verticalAlignButton, seeThroughButton, absoluteButton};
  }

  public ClickableWidget[] getToolbox3() {
    return new ClickableWidget[]{setCustomValueButton, rearrangeButton, finishButton, cancelButton, hideButton};
  }

  protected Collection<ButtonWidget> getTextHolders() {
    return List.of(placeHolder, doubleLineTemplateButton, leftArrowTemplateButton, rightArrowTemplateButton);
  }

  protected void initTextHolders() {
    for (ButtonWidget textHolder : getTextHolders()) {
      this.addDrawableChild(textHolder);
    }
  }

  /**
   * 更新初始屏幕（未添加文本时的按钮）与文本编辑框的可见性。初始化界面以及增删文本时，均调用此方法。
   */
  protected void updateTextHoldersVisibility() {
    final boolean visible = textFieldListWidget.children().isEmpty();
    for (ButtonWidget textHolder : getTextHolders()) {
      textHolder.visible = visible;
    }

    // 同时也需要更新 textFieldListWidget 的可见性
    textFieldListWidget.active = !visible;
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    if (hidden) {
      // 在隐藏界面时显示"显示界面"按钮
      showButton.render(context, mouseX, mouseY, delta);
      return;
    }
    
    // 不调用super.render()来避免背景模糊，直接渲染组件
    for (Element element : this.children()) {
      if (element instanceof Drawable drawable) {
        drawable.render(context, mouseX, mouseY, delta);
      }
    }
    

  }

  /**
   * 添加一个新的文本框。
   */
  public void addTextField(int index, boolean multiSel) {
    // 添加时，默认相当于上一行的。
    final TextContext emptyTextContext = index > 0 ? textContextsEditing.get(index - 1).clone() : entity.createDefaultTextContext();
    emptyTextContext.text = null;
    // 为新行添加默认行距
    if (index > 0) {
      // 根据前一行的字体大小计算行距，通常为字体大小的1.2-1.5倍
      float lineSpacing = Math.max(textContextsEditing.get(index - 1).size * 1.3f, 8f);
      emptyTextContext.offsetY = textContextsEditing.get(index - 1).offsetY + lineSpacing;
    }
    addTextField(index, emptyTextContext, false, multiSel);
  }

  /**
   * 添加一个文本框。
   */
  public void addTextField(int index, @NotNull TextContext textContext, boolean isExisting) {
    addTextField(index, textContext, isExisting, false);
  }

  /**
   * 添加一个文本框。
   */
  public void addTextField(int index, @NotNull TextContext textContext, boolean isExisting, boolean multiSel) {
    if (!isExisting) {
      textContextsEditing.add(index, textContext);
      changed = true;
    }
    final TextFieldWidget textFieldWidget = new TextFieldWidget(textRenderer, 2, 0, width - 4, 15, TextBridge.empty());
    textFieldWidget.setMaxLength(Integer.MAX_VALUE);
    if (textContext.text != null) {
      if (textContext.text.getContent() instanceof PlainTextContent plainTextContent && textContext.text.getSiblings().isEmpty() && textContext.text.getStyle().isEmpty()) {
        final String text = plainTextContent.string();
        if (Pattern.compile("^-(\\w+?) (.+)$").matcher(text).matches()) {
          textFieldWidget.setText("-literal " + text);
        } else {
          textFieldWidget.setText(text);
        }
      } else {
        textFieldWidget.setText("-json" + Text.Serialization.toJsonString(textContext.text, registryLookup));
      }
    }
    final TextFieldListWidget.Entry newEntry = textFieldListWidget.new Entry(textFieldWidget, textContext);
    textFieldListWidget.children().add(index, newEntry);
    textFieldListWidget.setFocused(newEntry, multiSel, false);
    textFieldListWidget.setScrollAmount(textFieldListWidget.getScrollAmount());
    if (!textFieldListWidget.children().isEmpty()) {
      setFocused(textFieldListWidget);
    }
    textFieldWidget.setChangedListener(s -> {
      final TextContext textContext1 = newEntry.textContext;
      final Matcher matcher = Pattern.compile("^-(\\w+?) (.+)$").matcher(s);
      if (matcher.matches()) {
        final String name = matcher.group(1);
        final String value = matcher.group(2);
        switch (name) {
          case "literal":
            textContext1.text = TextBridge.literal(value);
            break;
          case "json":
            try {
              textContext1.text = Text.Serialization.fromLenientJson(value, registryLookup);
            } catch (JsonParseException e) {
              // 如果文本有问题，则不执行操作。
            }
            break;
          default:
            textContext1.text = TextBridge.literal(s);
        }
      } else {
        textContext1.text = TextBridge.literal(s);
      }
      changed = true;
    });

    updateTextHoldersVisibility();
  }

  /**
   * 切换底部按钮的显示。显示高级按钮，或者取消高级按钮的显示。
   */
  private void arrangeToolboxButtons() {
    // 调整按钮位置
    arrangeToolboxButtons(getToolboxTop(), 3);
    arrangeToolboxButtons(getToolboxTemplates(), 28);  // 新增的模板按钮排
    arrangeToolboxButtons(getToolbox1(), height - 63);
    arrangeToolboxButtons(getToolbox2(), height - 43);
    arrangeToolboxButtons(getToolbox3(), height - 23);
  }

  /**
   * 调整一组按钮的位置，使其依次相邻，并总共居中显示。
   */
  private void arrangeToolboxButtons(ClickableWidget[] widgets, int y) {
    int accumulatedWidth = 0;
    for (ClickableWidget widget : widgets) {
      final int width = widget.getWidth();
      widget.setX(accumulatedWidth);
      accumulatedWidth += width;
    }
    for (ClickableWidget widget : widgets) {
      // 对于模板按钮，只在没有文本时显示
      if (widget == doubleLineTemplateButton || widget == leftArrowTemplateButton || widget == rightArrowTemplateButton) {
        widget.visible = textFieldListWidget.children().isEmpty();
      } else {
        widget.visible = true;
      }
      widget.setPosition(widget.getX() + width / 2 - accumulatedWidth / 2, y);
    }
  }

  /**
   * 移除一个文本框。
   */
  public void removeTextField(int index, boolean focusNearby) {
    final List<TextFieldListWidget.Entry> children = textFieldListWidget.children();
    final TextFieldListWidget.Entry removedEntry = children.remove(index);
    final TextFieldWidget removedWidget = removedEntry.textFieldWidget;
    final TextContext removedTextContext = removedEntry.textContext;
    if (textFieldListWidget.getSelectedOrNull() != null
        && removedWidget == textFieldListWidget.getSelectedOrNull().textFieldWidget) {
      textFieldListWidget.setFocused(null, true, false);
    }
    if (!children.isEmpty() && focusNearby) {
      textFieldListWidget.setFocused(children.get(MathHelper.clamp(index - 1, 0, children.size() - 1)), true, false);
    }
    // 删除一行元素后，对滚动数量进行一次 clamp，以避免出现过度滚动的情况。
    textFieldListWidget.setScrollAmount(textFieldListWidget.getScrollAmount());
    textContextsEditing.remove(removedTextContext);

    updateTextHoldersVisibility();
    changed = true;
  }

  private void finishEditing() {
    // Send the editing result to server
    ClientPlayNetworking.send(new SignEditFinishPayload(blockPos, changed ? createNbtForSending() : new NbtCompound()));
    if (this.client != null) {
      this.client.setScreen(null);
    }
  }

  protected NbtCompound createNbtForSending() {
    NbtCompound nbt = new NbtCompound();
    if (DEBUG == 1) System.out.println("Creating NBT for sending. Text contexts: " + textContextsEditing.size());
    if (textContextsEditing.size() == 1) {
      final NbtCompound nbtCompound = new NbtCompound();
      textContextsEditing.get(0).writeNbt(nbtCompound, registryLookup);
      nbt.put("text", nbtCompound);
      if (DEBUG == 1) System.out.println("Single text NBT: " + nbtCompound);
    } else {
      final NbtList nbtList = new NbtList();
      for (TextContext textContext : textContextsEditing) {
        nbtList.add(textContext.createNbt(registryLookup));
      }
      nbt.put("text", nbtList);
      if (DEBUG == 1) System.out.println("Multiple text NBT: " + nbtList);
    }
    if (DEBUG == 1) System.out.println("Final NBT: " + nbt);
    return nbt;
  }

  @Override
  public void removed() {
    super.removed();
    // 如果界面被关闭时有未保存的更改，发送空NBT表示取消
    if (changed) {
      ClientPlayNetworking.send(new SignEditFinishPayload(blockPos, new NbtCompound()));
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    // 如果界面隐藏，只处理showButton的点击
    if (hidden) {
      if (showButton.mouseClicked(mouseX, mouseY, button)) {
        return true;
      }
      return false;
    }
    
    for (Element element : this.children()) {
      if (isSelectingButtonToSetCustom && element instanceof FloatButtonWidget floatButtonWidget) {
        if (element.isMouseOver(mouseX, mouseY)) {
          floatButtonWidget.playDownSound(MinecraftClient.getInstance().getSoundManager());
          customValueStartAccepting(floatButtonWidget);
          return true;
        } else {
          continue;
        }
      }
      if (!element.mouseClicked(mouseX, mouseY, button))
        continue;
      if (element == textFieldListWidget || element instanceof TextFieldWidget) {
        this.setFocused(element);
      } else {
        setFocused(textFieldListWidget);
      }
      if (button == 0) {
        this.setDragging(true);
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      this.cancelEditing();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void tick() {
    super.tick();
  }

  // 自定义值设置相关方法
  private void customValueStartAccepting(FloatButtonWidget floatButtonWidget) {
    isSelectingButtonToSetCustom = false;
    for (Element child : children()) {
      if (child instanceof ClickableWidget clickableWidget) {
        clickableWidget.visible = clickableWidget == floatButtonWidget;
      }
    }
    isAcceptingCustomValue = true;
    customValueTextField.setEditableColor(16777215);
    customValueTextField.setSuggestion(null);
    // 在clearAndInit之前确定按钮类型
    if (floatButtonWidget == colorButton) {
      customValueForType = "color";
    } else if (floatButtonWidget == outlineColorButton) {
      customValueForType = "outlineColor";
    } else {
      customValueForType = "number";
    }
    
    clearAndInit();
    setFocused(customValueTextField);
    customValueFor = floatButtonWidget;
    
    if ("color".equals(customValueForType)) {
      if (!selectedTextContexts.isEmpty()) {
        customValueTextField.setText(PerfectSignsUtils.formatColorHex(textFieldListWidget.getFocused().textContext.color));
        customValueBeforeChange = (float) textFieldListWidget.getFocused().textContext.color;
      } else {
        customValueTextField.setText("");
        customValueBeforeChange = null;
      }
      customValueTextField.setChangedListener(s -> {
        final String text = customValueTextField.getText();
        if (DEBUG == 1) System.out.println("Color input changed: " + text);
        if (text.isEmpty()) {
          customValueTextField.setSuggestion(null);
          return;
        }
        final Integer parse = PerfectSignsUtils.parseColor(text).result().orElse(null);
        if (DEBUG == 1) System.out.println("Parsed color result: " + parse);
        if (parse == null) {
          customValueTextField.setEditableColor(16733525); // 红色表示无效输入
          if (DEBUG == 1) System.out.println("Color parsing failed");
        } else {
          customValueTextField.setEditableColor(16777215); // 白色表示有效输入
          if (DEBUG == 1) System.out.println("Color parsing succeeded: " + parse);
          for (TextContext textContext : selectedTextContexts) {
            textContext.color = parse;
          }
          changed = true;
        }
      });
    } else if ("outlineColor".equals(customValueForType)) {
      if (!selectedTextContexts.isEmpty()) {
        if (textFieldListWidget.getFocused().textContext.outlineColor == -1) {
          customValueTextField.setText("auto");
          customValueBeforeChange = -0.125f;
        } else if (textFieldListWidget.getFocused().textContext.outlineColor == -2) {
          customValueTextField.setText("none");
          customValueBeforeChange = -0.25f;
        } else {
          customValueTextField.setText(PerfectSignsUtils.formatColorHex(textFieldListWidget.getFocused().textContext.outlineColor));
          customValueBeforeChange = (float) textFieldListWidget.getFocused().textContext.outlineColor;
        }
      } else {
        customValueTextField.setText("");
        customValueBeforeChange = null;
      }
      customValueTextField.setChangedListener(s -> {
        final String text = customValueTextField.getText();
        if (text.isEmpty()) {
          customValueTextField.setSuggestion(null);
          return;
        }
        if (text.equalsIgnoreCase("auto")) {
          for (TextContext textContext : selectedTextContexts) {
            textContext.outlineColor = -1;
          }
          customValueTextField.setEditableColor(16777215);
          changed = true;
          return;
        } else if (text.equalsIgnoreCase("none")) {
          for (TextContext textContext : selectedTextContexts) {
            textContext.outlineColor = -2;
          }
          customValueTextField.setEditableColor(16777215);
          changed = true;
          return;
        }
        final Integer parse = PerfectSignsUtils.parseColor(text).result().orElse(null);
        if (parse == null) {
          customValueTextField.setEditableColor(16733525); // 红色表示无效输入
        } else {
          customValueTextField.setEditableColor(16777215); // 白色表示有效输入
          for (TextContext textContext : selectedTextContexts) {
            textContext.outlineColor = parse;
          }
          changed = true;
        }
      });
    } else {
      // 处理数值按钮的自定义值设置
      customValueBeforeChange = floatButtonWidget.getValue();
      if (customValueBeforeChange != null) {
        customValueTextField.setText(PerfectSignsUtils.numberToString(customValueBeforeChange));
      } else {
        customValueTextField.setText("");
      }
      customValueTextField.setChangedListener(s -> {
        try {
          final float value = Float.parseFloat(s);
          customValueTextField.setEditableColor(16777215);
          floatButtonWidget.setAllSameValue(value);
        } catch (NumberFormatException e) {
          customValueTextField.setEditableColor(16733525);
        }
      });
    }
  }

  private void customValueStopAccepting() {
    isSelectingButtonToSetCustom = false;
    customValueFor = null;
    customValueForType = null;
    customValueBeforeChange = null;
    customValueTextField.setChangedListener(s -> {});
    isAcceptingCustomValue = false;
    for (Element child : children()) {
      if (child instanceof ClickableWidget clickableWidget) {
        clickableWidget.visible = true;
      }
    }
    clearAndInit();
  }

  @Override
  protected void clearAndInit() {
    final double scrollAmountBeforeClear = textFieldListWidget == null ? -1 : textFieldListWidget.getScrollAmount();
    final Element previousFocused = getFocused();
    final TextFieldListWidget.Entry previouslyWidgetFocused = textFieldListWidget == null ? null : textFieldListWidget.getFocused();
    final List<TextFieldListWidget.Entry> selectedEntriesCopy = textFieldListWidget == null ? List.of() : List.copyOf(textFieldListWidget.selectedEntries);
    super.clearAndInit();
    setFocused(previousFocused);
    if (textFieldListWidget != null) {
      textFieldListWidget.setScrollAmount(scrollAmountBeforeClear);
      textFieldListWidget.setFocused(previouslyWidgetFocused);
      textFieldListWidget.selectedEntries.clear();
      textFieldListWidget.selectedEntries.addAll(selectedEntriesCopy);
      for (TextFieldListWidget.Entry selectedEntry : textFieldListWidget.selectedEntries) {
        selectedEntry.setFocused(true);
      }
    }
  }

  // 预设模板方法
  private void applyDoubleLineTemplate() {
    // 清空现有文本
    textContextsEditing.clear();
    textFieldListWidget.children().clear();
    
    // 创建第一行文本
    TextContext firstLine = entity.createDefaultTextContext();
    firstLine.text = TextBridge.literal("第一行文本");
    firstLine.offsetY = -5f;
    
    // 创建第二行文本
    TextContext secondLine = entity.createDefaultTextContext();
    secondLine.text = TextBridge.literal("第二行文本");
    secondLine.offsetY = 5f;
    
    // 添加到列表
    textContextsEditing.add(firstLine);
    textContextsEditing.add(secondLine);
    
    // 重新初始化界面
    clearAndInit();
    changed = true;
  }
  
  private void applyLeftArrowTemplate() {
    // 清空现有文本
    textContextsEditing.clear();
    textFieldListWidget.children().clear();
    
    // 创建左箭头文本
    TextContext arrowText = entity.createDefaultTextContext();
    arrowText.text = TextBridge.literal("← 左箭头");
    arrowText.offsetX = 0f;
    arrowText.offsetY = 0f;
    
    // 添加到列表
    textContextsEditing.add(arrowText);
    
    // 重新初始化界面
    clearAndInit();
    changed = true;
  }
  
  private void applyRightArrowTemplate() {
    // 清空现有文本
    textContextsEditing.clear();
    textFieldListWidget.children().clear();
    
    // 创建右箭头文本
    TextContext arrowText = entity.createDefaultTextContext();
    arrowText.text = TextBridge.literal("右箭头 →");
    arrowText.offsetX = 0f;
    arrowText.offsetY = 0f;
    
    // 添加到列表
    textContextsEditing.add(arrowText);
    
    // 重新初始化界面
    clearAndInit();
    changed = true;
  }
} 