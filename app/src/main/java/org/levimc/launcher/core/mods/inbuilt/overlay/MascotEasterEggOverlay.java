package org.levimc.launcher.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.levimc.launcher.R;

import java.util.Random;

public class MascotEasterEggOverlay {
    private static final int FRAME_DELAY = 180;
    private static final int LURK_SIZE_DP = 48;
    private static final int WALK_SIZE_DP = 64;
    private static final int IDLE_SIZE_DP = 80;
    private static final int MASCOT_SIZE_DP = 88;
    private static final int SPEECH_PADDING_DP = 10;
    private static final int SPEECH_HEIGHT_DP = 44;

    private enum State {
        LURKING,
        EMERGING,
        WAVING,
        REJECTED,
        WALKING,
        IDLE,
        RUNNING_TO_BUTTON,
        BLOCKING,
        APPROVED,
        HIDDEN
    }

    private final Activity activity;
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private FrameLayout containerView;
    private ImageView mascotView;
    private TextView speechBubble;
    private WindowManager.LayoutParams wmParams;

    private State currentState = State.HIDDEN;
    private boolean isShowing = false;
    private int posX;
    private int baseY;
    private int screenWidth, screenHeight;
    private int lurkSize;
    private int walkSize;
    private int idleSize;
    private int mascotSize;
    private int speechHeight;
    private int containerWidth;
    private int containerHeight;
    private int anchorX, anchorY;
    private int anchorIconSize;
    private float density;

    private int animFrame = 0;
    private boolean facingRight = false;
    private int dirX = 0;
    private int launchAttempts = 0;
    private int requiredAttempts;
    private int idleCounter = 0;
    private int walkCounter = 0;
    private static final int IDLE_DURATION = 8;
    private static final int WALK_DURATION_MIN = 15;
    private static final int WALK_DURATION_MAX = 30;
    private int currentWalkDuration;

    private int[] walkLeftFrames;
    private int[] walkRightFrames;
    private int[] runLeftFrames;
    private int[] runRightFrames;

    private Runnable pendingLaunchAction;

    private final String[] blockingMessages = {
        "Nope!",
        "Not yet!",
        "Try again~",
        "Hmm... no!",
        "Maybe later?",
        "Are you sure?",
        "One more time!",
        "Almost there..."
    };

    private final int[] blockingReactions = {
        R.drawable.mascot_angry,
        R.drawable.mascot_shrug,
        R.drawable.mascot_happy,
        R.drawable.mascot_reject,
        R.drawable.mascot_normal
    };

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isShowing) return;
            updateAnimation();
            handler.postDelayed(this, FRAME_DELAY);
        }
    };

    public MascotEasterEggOverlay(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
        initFrames();
    }

    private void initFrames() {
        walkLeftFrames = new int[] { R.drawable.mascot_walk_left_1, R.drawable.mascot_walk_left_2 };
        walkRightFrames = new int[] { R.drawable.mascot_walk_right_1, R.drawable.mascot_walk_right_2 };
        runLeftFrames = new int[] { R.drawable.mascot_run_left_1, R.drawable.mascot_run_left_2, R.drawable.mascot_run_left_3 };
        runRightFrames = new int[] { R.drawable.mascot_run_right_1, R.drawable.mascot_run_right_2, R.drawable.mascot_run_right_3 };
    }

    public void show(View anchorView) {
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;

        density = activity.getResources().getDisplayMetrics().density;
        lurkSize = (int) (LURK_SIZE_DP * density);
        walkSize = (int) (WALK_SIZE_DP * density);
        idleSize = (int) (IDLE_SIZE_DP * density);
        mascotSize = (int) (MASCOT_SIZE_DP * density);
        speechHeight = (int) (SPEECH_HEIGHT_DP * density);
        screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        anchorIconSize = anchorView.getHeight();
        anchorX = location[0];
        anchorY = location[1];

        requiredAttempts = 3 + random.nextInt(3);

        containerView = new FrameLayout(activity);
        containerWidth = mascotSize + (int)(180 * density);
        containerHeight = mascotSize + speechHeight;

        mascotView = new ImageView(activity);
        mascotView.setImageResource(R.drawable.mascot_lurk);
        mascotView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        speechBubble = new TextView(activity);
        speechBubble.setBackgroundResource(R.drawable.bg_speech_bubble);
        int padding = (int)(SPEECH_PADDING_DP * density);
        speechBubble.setPadding(padding, padding / 2, padding, padding / 2);
        speechBubble.setTextColor(0xFF333333);
        speechBubble.setTextSize(11);
        speechBubble.setVisibility(View.GONE);

        FrameLayout.LayoutParams mascotParams = new FrameLayout.LayoutParams(lurkSize, lurkSize);
        mascotParams.gravity = Gravity.BOTTOM | Gravity.START;
        containerView.addView(mascotView, mascotParams);

        FrameLayout.LayoutParams speechParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        speechParams.gravity = Gravity.TOP | Gravity.START;
        speechParams.leftMargin = lurkSize - (int)(8 * density);
        containerView.addView(speechBubble, speechParams);

        posX = anchorX + anchorIconSize - (int)(48 * density);
        baseY = anchorY - speechHeight;

        wmParams = new WindowManager.LayoutParams(
            containerWidth, containerHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        wmParams.gravity = Gravity.TOP | Gravity.START;
        wmParams.x = posX;
        wmParams.y = baseY;
        wmParams.token = activity.getWindow().getDecorView().getWindowToken();

        mascotView.setOnTouchListener(this::handleTouch);

        try {
            windowManager.addView(containerView, wmParams);
            isShowing = true;
            currentState = State.LURKING;
            handler.post(animationRunnable);
            startLurkingSequence();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMascotSize(int size) {
        FrameLayout.LayoutParams mascotParams = (FrameLayout.LayoutParams) mascotView.getLayoutParams();
        mascotParams.width = size;
        mascotParams.height = size;
        mascotView.setLayoutParams(mascotParams);
    }

    private void startLurkingSequence() {
        setMascotSize(lurkSize);
        mascotView.setImageResource(R.drawable.mascot_lurk);
        
        FrameLayout.LayoutParams speechParams = (FrameLayout.LayoutParams) speechBubble.getLayoutParams();
        speechParams.leftMargin = lurkSize - (int)(8 * density);
        speechBubble.setLayoutParams(speechParams);
        
        showSpeechBubble("wha..? did someone need me?");
        handler.postDelayed(this::startEmergingSequence, 2000);
    }

    private void startEmergingSequence() {
        if (!isShowing) return;
        currentState = State.EMERGING;
        hideSpeechBubble();
        handler.postDelayed(this::startWavingSequence, 400);
    }

    private void startWavingSequence() {
        if (!isShowing) return;
        currentState = State.WAVING;
        
        setMascotSize(mascotSize);
        mascotView.setImageResource(R.drawable.mascot_wave);
        
        FrameLayout.LayoutParams speechParams = (FrameLayout.LayoutParams) speechBubble.getLayoutParams();
        speechParams.leftMargin = mascotSize - (int)(16 * density);
        speechBubble.setLayoutParams(speechParams);
        
        showSpeechBubble("hello there! I'm going to annoy you a bit~");

        handler.postDelayed(() -> {
            if (currentState == State.WAVING) {
                hideSpeechBubble();
                handler.postDelayed(this::startWalkingFromCurrentPosition, 300);
            }
        }, 3000);
    }

    private void startWalkingFromCurrentPosition() {
        if (!isShowing) return;
        currentState = State.WALKING;

        setMascotSize(walkSize);
        mascotView.setImageResource(R.drawable.mascot_normal);
        
        facingRight = random.nextBoolean();
        dirX = facingRight ? (int)(2 * density) : (int)(-2 * density);
        currentWalkDuration = WALK_DURATION_MIN + random.nextInt(WALK_DURATION_MAX - WALK_DURATION_MIN);
        walkCounter = 0;
    }

    private void transitionToIdle() {
        currentState = State.IDLE;
        setMascotSize(idleSize);
        mascotView.setImageResource(R.drawable.mascot_normal);
        idleCounter = 0;
    }

    private void transitionToWalking() {
        currentState = State.WALKING;
        setMascotSize(walkSize);
        facingRight = random.nextBoolean();
        dirX = facingRight ? (int)(2 * density) : (int)(-2 * density);
        currentWalkDuration = WALK_DURATION_MIN + random.nextInt(WALK_DURATION_MAX - WALK_DURATION_MIN);
        walkCounter = 0;
    }

    private boolean handleTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (currentState == State.WAVING || currentState == State.WALKING || currentState == State.IDLE) {
                showRejectedReaction();
                return true;
            }
        }
        return false;
    }

    private void showRejectedReaction() {
        currentState = State.REJECTED;
        
        setMascotSize(mascotSize);
        mascotView.setImageResource(R.drawable.mascot_reject);
        
        FrameLayout.LayoutParams speechParams = (FrameLayout.LayoutParams) speechBubble.getLayoutParams();
        speechParams.leftMargin = mascotSize - (int)(16 * density);
        speechBubble.setLayoutParams(speechParams);
        
        showSpeechBubble("you pervert");

        handler.postDelayed(() -> {
            hideSpeechBubble();
            handler.postDelayed(this::transitionToWalking, 300);
        }, 1500);
    }

    private void showSpeechBubble(String message) {
        speechBubble.setText(message);
        speechBubble.setVisibility(View.VISIBLE);
        speechBubble.setAlpha(0f);
        speechBubble.animate().alpha(1f).setDuration(200).start();
    }

    private void hideSpeechBubble() {
        speechBubble.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            speechBubble.setVisibility(View.GONE);
        }).start();
    }

    private void updateAnimation() {
        if (mascotView == null || !isShowing) return;

        switch (currentState) {
            case WALKING:
                walkCounter++;
                if (walkCounter >= currentWalkDuration) {
                    transitionToIdle();
                } else {
                    moveAndAnimate(walkLeftFrames, walkRightFrames);
                }
                break;
            case IDLE:
                idleCounter++;
                if (idleCounter >= IDLE_DURATION) {
                    transitionToWalking();
                }
                break;
            case RUNNING_TO_BUTTON:
                runTowardsButton();
                break;
            default:
                break;
        }
    }

    private void moveAndAnimate(int[] leftFrames, int[] rightFrames) {
        posX += dirX;

        int margin = (int)(10 * density);
        if (posX < margin) {
            posX = margin;
            dirX = Math.abs(dirX);
            facingRight = true;
        } else if (posX > screenWidth - mascotSize - margin) {
            posX = screenWidth - mascotSize - margin;
            dirX = -Math.abs(dirX);
            facingRight = false;
        }

        animFrame = (animFrame + 1) % 2;
        int[] frames = facingRight ? rightFrames : leftFrames;
        mascotView.setImageResource(frames[animFrame % frames.length]);

        wmParams.x = posX;
        try {
            windowManager.updateViewLayout(containerView, wmParams);
        } catch (Exception ignored) {}
    }

    private int targetX;

    public boolean onLaunchButtonClicked(View launchButton, Runnable launchAction) {
        if (!isShowing || currentState == State.APPROVED || currentState == State.HIDDEN) {
            return false;
        }
        
        if (currentState == State.RUNNING_TO_BUTTON || currentState == State.BLOCKING) {
            return true;
        }

        this.pendingLaunchAction = launchAction;

        int[] location = new int[2];
        launchButton.getLocationOnScreen(location);
        targetX = location[0] - mascotSize / 2;

        launchAttempts++;

        if (launchAttempts >= requiredAttempts) {
            showApprovalAndLaunch();
            return true;
        }

        setMascotSize(mascotSize);
        currentState = State.RUNNING_TO_BUTTON;
        facingRight = targetX > posX;
        dirX = facingRight ? (int)(12 * density) : (int)(-12 * density);

        return true;
    }

    private void runTowardsButton() {
        if (Math.abs(posX - targetX) < Math.abs(dirX) * 2) {
            posX = targetX;
            currentState = State.BLOCKING;
            showBlockingReaction();
            return;
        }

        posX += dirX;
        animFrame = (animFrame + 1) % 3;
        int[] frames = facingRight ? runRightFrames : runLeftFrames;
        mascotView.setImageResource(frames[animFrame]);

        wmParams.x = posX;
        try {
            windowManager.updateViewLayout(containerView, wmParams);
        } catch (Exception ignored) {}
    }

    private void showBlockingReaction() {
        setMascotSize(mascotSize);
        
        int reactionIndex = random.nextInt(blockingReactions.length);
        mascotView.setImageResource(blockingReactions[reactionIndex]);

        FrameLayout.LayoutParams speechParams = (FrameLayout.LayoutParams) speechBubble.getLayoutParams();
        speechParams.leftMargin = mascotSize - (int)(16 * density);
        speechBubble.setLayoutParams(speechParams);
        
        String message = blockingMessages[random.nextInt(blockingMessages.length)];
        showSpeechBubble(message);

        handler.postDelayed(() -> {
            hideSpeechBubble();
            handler.postDelayed(this::transitionToWalking, 300);
        }, 1500);
    }

    private void showApprovalAndLaunch() {
        currentState = State.APPROVED;
        
        setMascotSize(mascotSize);
        mascotView.setImageResource(R.drawable.mascot_approve);
        
        FrameLayout.LayoutParams speechParams = (FrameLayout.LayoutParams) speechBubble.getLayoutParams();
        speechParams.leftMargin = mascotSize - (int)(16 * density);
        speechBubble.setLayoutParams(speechParams);
        
        showSpeechBubble("Fine, go ahead~!");

        handler.postDelayed(() -> {
            hide();
            if (pendingLaunchAction != null) {
                pendingLaunchAction.run();
            }
        }, 1500);
    }

    public boolean isActive() {
        return isShowing && currentState != State.HIDDEN && currentState != State.APPROVED;
    }

    public void hide() {
        if (!isShowing) return;
        isShowing = false;
        currentState = State.HIDDEN;
        handler.removeCallbacks(animationRunnable);

        try {
            if (containerView != null && windowManager != null) {
                windowManager.removeView(containerView);
            }
        } catch (Exception ignored) {}

        containerView = null;
        mascotView = null;
        speechBubble = null;
        launchAttempts = 0;
        pendingLaunchAction = null;
    }
}