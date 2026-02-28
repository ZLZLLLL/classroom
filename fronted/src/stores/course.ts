import { defineStore } from 'pinia'
import { getCourseList, getCourseById, createCourse, updateCourse, deleteCourse } from '../api/course'
import type { Course, CourseForm } from '../api/course'

interface CourseState {
  courses: Course[]
  currentCourse: Course | null
  total: number
  loading: boolean
}

export const useCourseStore = defineStore('course', {
  state: (): CourseState => ({
    courses: [],
    currentCourse: null,
    total: 0,
    loading: false
  }),

  actions: {
    async fetchCourses(params?: { page?: number; size?: number; keyword?: string }) {
      this.loading = true
      try {
        const data = await getCourseList(params || {})
        this.courses = data.records
        this.total = data.total
        return data
      } finally {
        this.loading = false
      }
    },

    async fetchCourseById(id: number) {
      this.loading = true
      try {
        this.currentCourse = await getCourseById(id)
        return this.currentCourse
      } finally {
        this.loading = false
      }
    },

    async addCourse(form: CourseForm) {
      const course = await createCourse(form)
      this.courses.unshift(course)
      return course
    },

    async editCourse(id: number, form: CourseForm) {
      const course = await updateCourse(id, form)
      const index = this.courses.findIndex(c => c.id === id)
      if (index !== -1) {
        this.courses[index] = course
      }
      if (this.currentCourse?.id === id) {
        this.currentCourse = course
      }
      return course
    },

    async removeCourse(id: number) {
      await deleteCourse(id)
      this.courses = this.courses.filter(c => c.id !== id)
    }
  }
})
