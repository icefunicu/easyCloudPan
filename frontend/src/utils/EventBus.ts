
import { ref } from 'vue';

type Callback = (payload?: any) => void;

class EventBus {
    private events: Record<string, Callback[]>;

    constructor() {
        this.events = {};
    }

    on(eventName: string, callback: Callback) {
        if (!this.events[eventName]) {
            this.events[eventName] = [];
        }
        this.events[eventName].push(callback);
    }

    off(eventName: string, callback: Callback) {
        if (!this.events[eventName]) return;
        this.events[eventName] = this.events[eventName].filter((cb) => cb !== callback);
    }

    emit(eventName: string, payload?: any) {
        if (!this.events[eventName]) return;
        this.events[eventName].forEach((callback) => callback(payload));
    }
}

export default new EventBus();
