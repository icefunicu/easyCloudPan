<template>
    <div class="no-data">
        <div>
            <Icon icon-name="no_data" :width="120" fit="fill"></Icon>
        </div>
        <div class="msg">{{ msg }}</div>
    </div>
</template>

<script setup>
const props = defineProps({
    msg: {
        type: String,
    },
});
</script>

<style lang="scss" scoped>
.no-data {
    text-align: center;
    padding: 20px 0;
    
    .icon-empty {
        font-size: 50px;
        color: var(--text-light);
    }
    
    .msg {
        margin-top: 15px;
        color: var(--text-secondary);
        font-size: 14px;
    }
}
</style>