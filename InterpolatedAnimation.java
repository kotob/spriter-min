package com.brashmonkey.spritermin;

import java.util.HashMap;

/**
 * Created by kot on 20.06.16.
 */
public class InterpolatedAnimation extends Animation{

    Entity entity;
    Curve curve;
    Animation anim;
    HashMap<Integer, Timeline.Key.Bone> stateMap;
    int animTime;
    int animSpeed = 15;


    public float weight = .5f;


    public InterpolatedAnimation(Entity entity) {
        super(new Mainline(0), -1, "__interpolatedStateAnimation__", 0, true, entity.getAnimationWithMostTimelines().timelines());
        this.entity = entity;
        this.curve = new Curve();
        this.setUpTimelines();
        stateMap = new HashMap<Integer, Timeline.Key.Bone>();
    }

    public void setAnimation(String animationName){
        stateMap.clear();
        if(currentKey!=null) {
            for (Mainline.Key.BoneRef ref : currentKey.boneRefs) {
                Timeline t = super.getTimeline(ref.timeline);
                Timeline.Key.Bone bone = this.tweenedKeys[t.id].object();
                if(bone != null) stateMap.put(ref.id, bone);
            }
        }
        this.animTime = 0;
        this.anim = entity.getAnimation(animationName);
    }


    public HashMap<Integer, Timeline.Key.Bone> getState(){
        HashMap<Integer, Timeline.Key.Bone> boneMap = new HashMap<Integer, Timeline.Key.Bone>();
        if(currentKey!=null) {
            for (Mainline.Key.BoneRef ref : currentKey.boneRefs) {
                Timeline t = super.getTimeline(ref.timeline);
                Timeline.Key.Bone bone = this.tweenedKeys[t.id].object();
                if(bone != null)boneMap.put(ref.id, bone);
            }
        }
        return boneMap;
    }

    private void setUpTimelines(){
        Animation maxAnim = this.entity.getAnimationWithMostTimelines();
        int max = maxAnim.timelines();
        for(int i = 0; i < max; i++){
            Timeline t = new Timeline(i, maxAnim.getTimeline(i).name, maxAnim.getTimeline(i).objectInfo, 1);
            addTimeline(t);
        }
        prepare();
    }

    @Override
    public void update(int time, Timeline.Key.Bone root){
        animTime += animSpeed;
        if(animTime > anim.length) animTime -= anim.length;
        if(animTime < 0) animTime += anim.length;
        anim.update(animTime,root);
        //super.currentKey = onFirstMainLine() ? anim1.currentKey: anim2.currentKey;
        super.currentKey = anim.currentKey;
        for(Timeline.Key timelineKey: this.unmappedTweenedKeys)
            timelineKey.active = false;

        for(Mainline.Key.BoneRef ref: currentKey.boneRefs)
            this.update(ref, root, time);

        for(Mainline.Key.ObjectRef ref: super.currentKey.objectRefs) {
            this.update(ref, root, 0);
        }
    }

    @Override
    protected void update(Mainline.Key.BoneRef ref, Timeline.Key.Bone root, int time){
        boolean isObject = ref instanceof Mainline.Key.ObjectRef;
        //Tween bone/object
        Timeline.Key.Bone bone1 = null, bone2 = null, tweenTarget = null;
        Timeline targetTimeline = super.getTimeline(ref.timeline);
        //Timeline t1 = anim.getSimilarTimeline(targetTimeline);
        Timeline t1 = anim.getTimeline(ref.timeline);
        bone1 = anim.tweenedKeys[targetTimeline.id].object();
        if(!isObject)bone2 = stateMap.get(ref.id);
        if(bone2 == null || isObject)bone2 = bone1;
        tweenTarget = this.tweenedKeys[targetTimeline.id].object();

        if(bone2 != null && tweenTarget != null && bone1 != null){
            //if(!isObject)Gdx.app.debug("ISA", tweenTarget.toString());
            if(isObject) this.tweenObject((Timeline.Key.Object)bone1, (Timeline.Key.Object)bone2, (Timeline.Key.Object)tweenTarget, this.weight, this.curve);
            if(!isObject) this.tweenBone(bone1, bone2, tweenTarget, this.weight, this.curve);
            this.unmappedTweenedKeys[targetTimeline.id].active = true;
        }
        //Transform the bone relative to the parent bone or the root
        if(this.unmappedTweenedKeys[ref.timeline].active){
            this.unmapTimelineObject(targetTimeline.id, isObject,(ref.parent != null) ?
                    this.unmappedTweenedKeys[ref.parent.timeline].object(): root);
        }
    }

    private void tweenBone(Timeline.Key.Bone bone1, Timeline.Key.Bone bone2, Timeline.Key.Bone target, float t, Curve curve){
        target.angle = curve.tweenAngle(bone1.angle, bone2.angle, t);
        curve.tweenPoint(bone1.position, bone2.position, t, target.position);
        curve.tweenPoint(bone1.scale, bone2.scale, t, target.scale);
        curve.tweenPoint(bone1.pivot, bone2.pivot, t, target.pivot);
    }

    private void tweenObject(Timeline.Key.Object object1, Timeline.Key.Object object2, Timeline.Key.Object target, float t, Curve curve){
        this.tweenBone(object1, object2, target, t, curve);
        target.alpha = curve.tweenAngle(object1.alpha, object2.alpha, t);
        target.ref.set(object1.ref);
    }

}
